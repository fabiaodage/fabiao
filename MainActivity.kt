package com.ak.smstest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ak.smstest.data.CallLogEntry
import com.ak.smstest.data.SmsEntry
import com.ak.smstest.repository.CallLogRepository
import com.ak.smstest.repository.SmsRepository
import com.ak.smstest.security.HookDetector
import com.ak.smstest.security.HookDetectionResult
import com.ak.smstest.ui.theme.SMSTestTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = 
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                loadData()
            }
        }
    
    private var callLogs by mutableStateOf<List<CallLogEntry>>(emptyList())
    private var smsList by mutableStateOf<List<SmsEntry>>(emptyList())
    private var selectedTab by mutableStateOf(0)
    private var isLoading by mutableStateOf(false)
    private var hookDetectionResult by mutableStateOf<HookDetectionResult?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            SMSTestTheme {
                MainScreen()
            }
        }
        
        checkPermissionsAndLoadData()
    }
    
    private fun checkPermissionsAndLoadData() {
        val permissions = arrayOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS
        )
        
        val hasAllPermissions = permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
        
        if (hasAllPermissions) {
            loadData()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }
    
    private fun loadData() {
        isLoading = true
        
        val callLogRepository = CallLogRepository(this)
        val smsRepository = SmsRepository(this)
        
        callLogs = callLogRepository.getCallLogs()
        smsList = smsRepository.getSms()
        
        isLoading = false
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("通话记录和短信") }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    ) {
                        Text("通话记录", modifier = Modifier.padding(16.dp))
                    }
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    ) {
                        Text("短信", modifier = Modifier.padding(16.dp))
                    }
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    ) {
                        Text("Scheme测试", modifier = Modifier.padding(16.dp))
                    }
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    ) {
                        Text("Hook检测", modifier = Modifier.padding(16.dp))
                    }
                }
                
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    when (selectedTab) {
                        0 -> CallLogList(callLogs)
                        1 -> SmsList(smsList)
                        2 -> SchemeTestScreen()
                        3 -> HookDetectionScreen()
                    }
                }
            }
        }
    }
    
    @Composable
    fun CallLogList(callLogs: List<CallLogEntry>) {
        if (callLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无通话记录")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(callLogs) { callLog ->
                    CallLogItem(callLog)
                }
            }
        }
    }
    
    @Composable
    fun SmsList(smsList: List<SmsEntry>) {
        if (smsList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无短信记录")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(smsList) { sms ->
                    SmsItem(sms)
                }
            }
        }
    }
    
    @Composable
    fun CallLogItem(callLog: CallLogEntry) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = callLog.name ?: callLog.number,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = callLog.getTypeString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = callLog.number,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "时长: ${formatDuration(callLog.duration)}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = formatDate(callLog.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
    
    @Composable
    fun SmsItem(sms: SmsEntry) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = sms.address,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = sms.getTypeString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = sms.body,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatDate(sms.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
    
    @Composable
    fun SchemeTestScreen() {
        val context = LocalContext.current
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Scheme 测试",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Button(
                onClick = {
                    launchApp(context, "xkongjian://", "X")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("打开微信")
            }
            
            Button(
                onClick = {
                    launchApp(context, "alipays://", "支付宝")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("打开支付宝")
            }
            
            Button(
                onClick = {
                    launchApp(context, "taobao://", "淘宝")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("打开淘宝")
            }
            
            Button(
                onClick = {
                    launchApp(context, "mqq://", "QQ")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("打开QQ")
            }
            
            Button(
                onClick = {
                    launchApp(context, "tel:10086", "拨打电话")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("拨打10086")
            }
            
            Button(
                onClick = {
                    launchApp(context, "sms:10086", "发送短信")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("发送短信到10086")
            }
            
            Button(
                onClick = {
                    launchApp(context, "mailto:test@example.com", "发送邮件")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("发送邮件")
            }
            
            Button(
                onClick = {
                    launchApp(context, "https://www.baidu.com", "打开网页")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("打开百度")
            }
        }
    }
    
    private fun launchApp(context: android.content.Context, scheme: String, appName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scheme))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开$appName: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
            else -> String.format("%d:%02d", minutes, secs)
        }
    }
    
    @Composable
    fun HookDetectionScreen() {
        val context = LocalContext.current
        var isDetecting by remember { mutableStateOf(false) }
        var result by remember { mutableStateOf(hookDetectionResult) }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Hook 检测",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Button(
                onClick = {
                    isDetecting = true
                    try {
                        val detector = HookDetector(context)
                        result = detector.detectHooks()
                        hookDetectionResult = result
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isDetecting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDetecting
            ) {
                if (isDetecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("开始检测")
                }
            }
            
            if (result != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result!!.isHooked) 
                            MaterialTheme.colorScheme.errorContainer 
                        else 
                            MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (result!!.isHooked) "⚠️ 检测到Hook" else "✓ 环境安全",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (result!!.isHooked) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "检测详情:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(result!!.details.size) { index ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result!!.details[index],
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}