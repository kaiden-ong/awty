package edu.uw.ischool.kong314.awty

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.telephony.SmsManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val tag = "MainActivity"
    private val SEND_SMS_PERMISSION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)
        var validInputs = false
        var validMediaInputs = false
        button.text = "Start"
        var messageEmpty = true
        var phoneValid = true
        var numberEmpty = true
        val message = findViewById<EditText>(R.id.editText)
        val phone = findViewById<EditText>(R.id.editTextPhone)
        val number = findViewById<EditText>(R.id.editTextNumber)

        message.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                messageEmpty = p0.toString().trim().isEmpty()
                validInputs = !numberEmpty && !messageEmpty && phoneValid
                validMediaInputs = !numberEmpty && phoneValid
            }

            override fun afterTextChanged(p0: Editable?) {}
        })
        phone.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                phoneValid = p0.toString().trim().length == 10
                validInputs = !numberEmpty && !messageEmpty && phoneValid
                validMediaInputs = !numberEmpty && phoneValid
            }

            override fun afterTextChanged(p0: Editable?) {}
        })
        number.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                numberEmpty = p0.toString().trim().isEmpty()
                validInputs = !numberEmpty && !messageEmpty && phoneValid
                validMediaInputs = !numberEmpty && phoneValid
            }

            override fun afterTextChanged(p0: Editable?) {}
        })
        button.setOnClickListener() {
            if (validInputs) {
                val phoneNumber = phone.text.toString()
                val areaCode = phoneNumber.substring(0,3)
                val middleThree = phoneNumber.substring(3,6)
                val endingFour = phoneNumber.substring(6,10)
                val formattedNumber = "($areaCode) $middleThree-$endingFour"
                val messageText = message.text.toString()
                val minuteValue = number.text.toString().toLong() * 60000
                val toastMsg = "$formattedNumber:$messageText"
                val intent = Intent("Alarm")
                intent.putExtra("toastMsg", toastMsg)
                intent.putExtra("msg", messageText)
                intent.putExtra("phone", phoneNumber)
                val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

                if (button.text == "Start") {
                    sendBroadcast(intent)
                    Log.d(tag, "starting msg")
                    button.text = "Stop"
                    val triggerAtMillis = SystemClock.elapsedRealtime() + minuteValue
                    alarmManager.setRepeating(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        minuteValue,
                        pendingIntent
                    )
                } else {
                    Log.d(tag, "stopping msg")
                    button.text = "Start"
                    alarmManager.cancel(pendingIntent)
                }
            } else {
                Toast.makeText(this, "Invalid Parameters", Toast.LENGTH_SHORT).show()
            }
        }
        val broadcastReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val toastMsg = intent?.getStringExtra("toastMsg")
                val msg = intent?.getStringExtra("msg")
                val phoneNumber = intent?.getStringExtra("phone")
                if (toastMsg != null && msg != null && phoneNumber != null) {
                    Log.d("Main", "Sending toast: $toastMsg")
                    sendSMSMessage(phoneNumber, msg);
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
        val intentFilter = IntentFilter("Alarm")
        registerReceiver(broadcastReceiver, intentFilter)

//        val audioBtn = findViewById<Button>(R.id.button2)
//        val videoBtn = findViewById<Button>(R.id.button3)
//        val audioFile = copyAssetFileToCache(this, "awty_audio.mp3")
//        val videoFile = copyAssetFileToCache(this, "awty_audio.mp4")
//
//        audioBtn.setOnClickListener {
//            if (validMediaInputs) {
//                val intent = Intent("Audio")
//                intent.putExtra("phone", phone.text.toString())
//                val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
//                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
//                val minuteValue = number.text.toString().toLong() * 60000
//                if (audioBtn.text == "Start Audio") {
//                    sendBroadcast(intent)
//                    button.text = "Stop Audio"
//                    val triggerAtMillis = SystemClock.elapsedRealtime() + minuteValue
//                    alarmManager.setRepeating(
//                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                        triggerAtMillis,
//                        minuteValue,
//                        pendingIntent
//                    )
//                } else {
//                    button.text = "Start Audio"
//                    alarmManager.cancel(pendingIntent)
//                }
//                val phoneNumber = phone.text.toString()
//                sendSMSAudio(phoneNumber, audioFile!!)
//            }
//        }
//
//        videoBtn.setOnClickListener {
//            val phoneNumber = phone.text.toString()
//            sendSMSVideo(phoneNumber, videoFile!!)
//        }
    }

    private fun sendSMSMessage(phone: String, message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SEND_SMS_PERMISSION_REQUEST_CODE)
            return
        }

        var smsManager = applicationContext.getSystemService<SmsManager>(SmsManager::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            smsManager = SmsManager.getDefault()
        }
        val sentIntent = PendingIntent.getBroadcast(applicationContext,0,Intent("SMS_Sent"),
            PendingIntent.FLAG_IMMUTABLE)
        val deliveredIntent = PendingIntent.getBroadcast(applicationContext, 0, Intent("SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE)
        smsManager.sendTextMessage(phone, null, message, sentIntent, deliveredIntent)
    }

    private fun sendSMSAudio(phone: String, file: File) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SEND_SMS_PERMISSION_REQUEST_CODE)
            return
        }
        Log.d(tag, "Setting smsmanager")
        var smsManager = applicationContext.getSystemService<SmsManager>(SmsManager::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            smsManager = SmsManager.getDefault()
        }
        val sentIntent = PendingIntent.getBroadcast(applicationContext,0,Intent("SMS_Sent"),
            PendingIntent.FLAG_IMMUTABLE)
        val deliveredIntent = PendingIntent.getBroadcast(applicationContext, 0, Intent("SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE)
        Log.d(tag, "Sending audio")
        smsManager.sendMultimediaMessage(applicationContext, Uri.fromFile(file), null, null, sentIntent)
    }

//    private fun sendSMSVideo(phone: String, file: File) {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SEND_SMS_PERMISSION_REQUEST_CODE)
//            return
//        }
//
//        var smsManager = applicationContext.getSystemService<SmsManager>(SmsManager::class.java)
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
//            smsManager = SmsManager.getDefault()
//        }
//        val sentIntent = PendingIntent.getBroadcast(applicationContext,0,Intent("SMS_Sent"),
//            PendingIntent.FLAG_IMMUTABLE)
//        val deliveredIntent = PendingIntent.getBroadcast(applicationContext, 0, Intent("SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE)
//        smsManager.sendTextMessage(phone, null, message, sentIntent, deliveredIntent)
//    }

    private fun copyAssetFileToCache(context: Context, assetFileName: String): File? {
        return try {
            val inputStream = context.assets.open(assetFileName)
            val outputFile = File(context.cacheDir, assetFileName)
            val outputStream = FileOutputStream(outputFile)
            inputStream.copyTo(outputStream)
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            outputFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}