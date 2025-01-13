package com.example.bt_def.bluetooth
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*


class ConnectThread(
    private val context: Context,
    device: BluetoothDevice,
    val listener: BluetoothController.Listener
) :
    Thread() {
    private val uuid = "a4b8c2e0-5f29-11eb-ae93-0242ac130002"
    private var mSocket: BluetoothSocket? = null

    init {
        try {
            mSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(uuid))
            Log.d("MyLog", "mSocket start")
        } catch (e: IOException) {
            Log.e("MyLog", "mSocket IOException")
        } catch (se: SecurityException) {
            Log.e("MyLog", "mSocket SecurityException")
        }
    }

    override fun run() {
        try {
            Log.d("MyLog", "Attempting to connect socket")
            mSocket?.connect()
            Log.d("MyLog", "Socket connected successfully")
            listener.onReceive(BluetoothController.BLUETOOTH_CONNECTED)
            readMessage()
        } catch (e: IOException) {
            Log.e("MyLog", "Failed to connect socket", e)
            listener.onReceive(BluetoothController.BLUETOOTH_NO_CONNECTED)
        } catch (se: SecurityException) {
            Log.e("MyLog", "SecurityException during connection", se)
        }
    }

    private fun readMessage() {
        val inputStream = mSocket?.inputStream ?: return

        val buffer = ByteArray(1024)
        val base64Buffer = StringBuilder()
        var readingImage = false

        while (true) {
            try {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) {
                    // Конец потока
                    listener.onReceive(BluetoothController.BLUETOOTH_NO_CONNECTED)
                    break
                }
                val rawString = String(buffer, 0, bytesRead)
                if (!readingImage) {
                    if (rawString.contains("IMAGE_START")) {
                        readingImage = true
                        // Обрежем всё до IMAGE_START (если нужно)
                    } else {
                        // Обычные сообщения
                        listener.onReceive(rawString)
                    }
                } else {
                    // Уже читаем Base64
                    if (rawString.contains("IMAGE_END")) {
                        // Вырежем часть строки до IMAGE_END
                        val endIndex = rawString.indexOf("IMAGE_END")
                        val base64Part = rawString.substring(0, endIndex)
                        base64Buffer.append(base64Part)

                        // Декодируем всё, что накопилось
                        val imageBytes = Base64.getDecoder().decode(base64Buffer.toString())
                        saveImageToFile(imageBytes)
                        base64Buffer.clear()

                        readingImage = false
                        // Отправляем событие во фрагмент
                        listener.onReceive("IMAGE_RECEIVED")
                    } else {
                        // Просто добавляем очередную порцию Base64
                        base64Buffer.append(rawString)
                    }
                }
            } catch (e: IOException) {
                listener.onReceive(BluetoothController.BLUETOOTH_NO_CONNECTED)
                break
            }
        }
    }

//    private fun readMessage() {
//        val buffer = ByteArray(256)
//        val inputStream = mSocket?.inputStream ?: return
//
//        while (true) {
//            try {
//                // Считываем данные из потока
//                val length = inputStream.read(buffer)
//                if (length == -1) {
//                    // Конец потока
//                    listener.onReceive(BluetoothController.BLUETOOTH_NO_CONNECTED)
//                    break
//                }
//                val rawMessage = String(buffer, 0, length)
//
//                Log.d("MyLog", "Received message: $rawMessage")
//
//                // Проверим, не пришёл ли заголовок с информацией об изображении
//                if (rawMessage.startsWith("IMAGE_START:")) {
//                    // Парсим размер
//                    val parts = rawMessage.split(":")
//                    if (parts.size == 2) {
//                        val imageSize = parts[1].trim().toIntOrNull() ?: 0
//                        if (imageSize > 0) {
//                            // Теперь читаем бинарные данные ровно imageSize байт
//                            readImage(inputStream, imageSize)
//                            // После удачного чтения сообщаем фрагменту
//                            listener.onReceive("IMAGE_RECEIVED")
//                        }
//                    }
//                } else {
//                    // Если это не начало изображения, то работаем как прежде:
//                    listener.onReceive(rawMessage)
//                }
//            } catch (e: IOException) {
//                Log.e("MyLog", "Error while reading message", e)
//                listener.onReceive(BluetoothController.BLUETOOTH_NO_CONNECTED)
//                break
//            }
//        }
//    }

    /**
     * Сохраняем массив байт в файл.
     */
    private fun saveImageToFile(imageBytes: ByteArray) {
        try {
            // ВАЖНО: getExternalFilesDir() вызывается из Activity или Fragment
            // поэтому лучше передать контекст или другой механизм.
            // Для упрощения примера показываем, как можно «пробросить» путь.
            val file = File(context.getExternalFilesDir(null),
                "received_image.jpg"
            )
            file.outputStream().use { fos ->
                fos.write(imageBytes)
                fos.flush()
            }
            Log.d("MyLog", "Image saved to: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("MyLog", "Error saving image file", e)
        }
    }

//    private fun readMessage() {
//        val buffer = ByteArray(256)
//        while (true) {
//            try {
//                val length = mSocket?.inputStream?.read(buffer)
//                val message = String(buffer, 0, length ?: 0)
//                Log.d("MyLog", "Received message: $message")
//                listener.onReceive(message)
//            } catch (e: IOException) {
//                Log.e("MyLog", "Error while reading message", e)
//                listener.onReceive(BluetoothController.BLUETOOTH_NO_CONNECTED)
//                break
//            }
//        }
//    }


    fun sendMessage(message: String) {
        try {
            Log.d("MyLog", "Sending message: $message")
            mSocket?.outputStream?.write(message.toByteArray())
            Log.d("MyLog", "Message sent successfully")
        } catch (e: IOException) {
            Log.e("MyLog", "Error while sending message", e)
        }
    }


    fun closeConnection() {
        try {
            mSocket?.close()
        } catch (e: IOException) {

        }
    }
}