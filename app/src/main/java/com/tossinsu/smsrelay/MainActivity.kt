package com.tossinsu.smsrelay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.tossinsu.smsrelay.Prefs.matchesWhitelist
import com.tossinsu.smsrelay.Prefs.pairCode
import com.tossinsu.smsrelay.Prefs.role
import com.tossinsu.smsrelay.Prefs.whitelistRaw
import com.tossinsu.smsrelay.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var adapter: MessageAdapter

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { startByRole() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        NotificationHelper.ensureChannels(this)

        adapter = MessageAdapter()
        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter

        // 저장된 값 복원
        b.pairCode.setText(pairCode)
        b.whitelist.setText(whitelistRaw)
        when (role) {
            Prefs.ROLE_SENDER -> b.roleGroup.check(R.id.roleSender)
            Prefs.ROLE_RECEIVER -> b.roleGroup.check(R.id.roleReceiver)
        }
        updateRoleViews()

        b.roleGroup.setOnCheckedChangeListener { _, _ -> updateRoleViews() }
        b.saveBtn.setOnClickListener { onSave() }
        b.stopBtn.setOnClickListener { onStop() }
        b.clearBtn.setOnClickListener {
            MessageStore.clear(this); refreshList()
        }
        b.testBtn.setOnClickListener { onTestMatch() }

        MessageStore.onChanged = { runOnUiThread { refreshList() } }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun selectedRole(): String =
        if (b.roleGroup.checkedRadioButtonId == R.id.roleSender)
            Prefs.ROLE_SENDER else Prefs.ROLE_RECEIVER

    private fun updateRoleViews() {
        val sender = b.roleGroup.checkedRadioButtonId == R.id.roleSender
        b.whitelistBox.visibility = if (sender) View.VISIBLE else View.GONE
        b.testBtn.visibility = if (sender) View.VISIBLE else View.GONE
        b.listLabel.visibility = if (sender) View.GONE else View.VISIBLE
        b.list.visibility = if (sender) View.GONE else View.VISIBLE
        b.clearBtn.visibility = if (sender) View.GONE else View.VISIBLE
    }

    private fun onSave() {
        val code = b.pairCode.text?.toString()?.trim().orEmpty()
        if (code.isBlank()) {
            toast("페어 코드를 입력하세요 (두 폰에 동일하게)")
            return
        }
        pairCode = code
        role = selectedRole()
        if (selectedRole() == Prefs.ROLE_SENDER) {
            whitelistRaw = b.whitelist.text?.toString().orEmpty()
        }
        requestPermsThenStart()
    }

    private fun requestPermsThenStart() {
        val perms = mutableListOf<String>()
        if (selectedRole() == Prefs.ROLE_SENDER) {
            perms += Manifest.permission.RECEIVE_SMS
            perms += Manifest.permission.READ_SMS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permLauncher.launch(missing.toTypedArray())
        } else {
            startByRole()
        }
        askIgnoreBatteryOptimization()
    }

    private fun startByRole() {
        when (selectedRole()) {
            Prefs.ROLE_SENDER -> {
                SenderService.start(this); ReceiverService.stop(this)
                toast("SENDER 실행: 문자 감시를 시작합니다")
            }
            Prefs.ROLE_RECEIVER -> {
                ReceiverService.start(this); SenderService.stop(this)
                toast("RECEIVER 실행: 전달 문자 수신을 시작합니다")
            }
        }
        refreshList()
    }

    private fun onStop() {
        SenderService.stop(this); ReceiverService.stop(this)
        toast("서비스를 중지했습니다")
    }

    private fun onTestMatch() {
        // 미리 저장 없이 현재 입력값으로 테스트
        whitelistRaw = b.whitelist.text?.toString().orEmpty()
        val sample = b.testNumber.text?.toString().orEmpty()
        if (sample.isBlank()) { toast("테스트할 번호를 입력하세요"); return }
        val ok = matchesWhitelist(sample)
        toast(if (ok) "일치 ✓ 전달 대상입니다" else "불일치 ✗ 전달되지 않습니다")
    }

    private fun askIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val pm = getSystemService(PowerManager::class.java)
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        try {
            startActivity(Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            ))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun refreshList() {
        if (selectedRole() == Prefs.ROLE_RECEIVER) {
            adapter.submit(MessageStore.all(this))
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
