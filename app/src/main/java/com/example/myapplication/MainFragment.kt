package com.example.myapplication

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.bt_def.BluetoothConstants
import com.example.myapplication.databinding.FragmentMainBinding
import androidx.appcompat.content.res.AppCompatResources
import com.example.bt_def.bluetooth.BluetoothController
import java.io.File


class MainFragment : Fragment(), BluetoothController.Listener {
    private lateinit var binding: FragmentMainBinding
    private lateinit var bluetoothController: BluetoothController
    private lateinit var btAdapter: BluetoothAdapter
    private var maxValue: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBtAdapter()
        val pref = activity?.getSharedPreferences(
            BluetoothConstants.PREFERENCES, Context.MODE_PRIVATE)
        val mac = pref?.getString(BluetoothConstants.MAC, "")
        bluetoothController = BluetoothController(context = requireContext(), btAdapter)


        binding.bList.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_deviceListFragment)
        }

        binding.dataReciver.setOnClickListener{
            bluetoothController.sendMessage("request_image")
            val imageFile = File(requireContext().getExternalFilesDir(null), "received_image.jpg")
            if (imageFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                binding.imageGraph.setImageBitmap(bitmap)
            } else {
                Toast.makeText(context, "Файл изображения не найден", Toast.LENGTH_SHORT).show()
            }
        }

        binding.bConnect.setOnClickListener {
            bluetoothController.connect(mac ?: "" , this)
        }

        binding.bShowFullImage.setOnClickListener {
            // Проверим, есть ли уже полученный файл
            val imageFile = File(requireContext().getExternalFilesDir(null), "received_image.jpg")
            if (imageFile.exists()) {
                // Создаём интент, передаём путь к файлу
                val intent = Intent(requireContext(), DisplayImageActivity::class.java)
                intent.putExtra("image_path", imageFile.absolutePath)
                startActivity(intent)
            } else {
                Toast.makeText(context, "Файл изображения не найден", Toast.LENGTH_SHORT).show()
            }
        }

        binding.sendMaxValue.setOnClickListener{
            val maxValueToSend = getMaxValue()
            val formattedMessage = if (maxValueToSend > 0) {
                "max_value $maxValueToSend"
            } else {
                maxValueToSend
            }
            Toast.makeText(context, "Отправлено максимальное значение: $maxValueToSend", Toast.LENGTH_SHORT).show()
            bluetoothController.sendMessage(formattedMessage.toString())
        }

        binding.bSend.setOnClickListener {
            val message = binding.etMessage.text.toString()
            if (message.isEmpty()) {
                Toast.makeText(context, "Введите сообщение", Toast.LENGTH_SHORT).show()
                Log.w("Bluetooth", "Attempted to send empty message")
                return@setOnClickListener
            }

            if (!message.matches(Regex("\\d+"))) {
                Toast.makeText(context, "Можно отправить только число", Toast.LENGTH_SHORT).show()
                Log.w("Bluetooth", "Attempted to send non-numeric message")
                return@setOnClickListener
            }

            val formattedMessage = if (message.matches(Regex("\\d+"))) {
                "send_value $message"
            } else {
                message
            }
            Log.d("Bluetooth", "Sending formatted message: $formattedMessage")
            bluetoothController.sendMessage(formattedMessage)
            Toast.makeText(context, "Отправлено: $formattedMessage", Toast.LENGTH_SHORT).show()
            binding.etMessage.setText("")
        }

        binding.stopSend.setOnClickListener {
            bluetoothController.sendMessage("stop_motor")
        }

    }

    private fun initBtAdapter() {
        val bManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (bManager == null) {
            Log.e("Bluetooth", "BluetoothManager is null")
        } else {
            btAdapter = bManager.adapter
            if (btAdapter == null) {
                Log.e("Bluetooth", "BluetoothAdapter is null")
            } else {
                Log.d("Bluetooth", "BluetoothAdapter initialized successfully")
            }
        }
    }


    override fun onReceive(message: String) {
        activity?.runOnUiThread {
            when (message) {
                BluetoothController.BLUETOOTH_CONNECTED -> {
                    binding.bConnect.backgroundTintList = AppCompatResources
                        .getColorStateList(requireContext(), R.color.red)
                    binding.bConnect.text = "Disconnect"
                }
                BluetoothController.BLUETOOTH_NO_CONNECTED -> {
                    binding.bConnect.backgroundTintList = AppCompatResources
                        .getColorStateList(requireContext(), R.color.green)
                    binding.bConnect.text = "Connect"
                }
//                "IMAGE_RECEIVED" -> {
//                    // Как только вся картинка пришла, отобразим её:
//                    val imageFile = File(requireContext().getExternalFilesDir(null), "received_image.jpg")
//                    if (imageFile.exists()) {
//                        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
//                        binding.imageGraph.setImageBitmap(bitmap)
//                        Toast.makeText(context, "Изображение получено и отображено", Toast.LENGTH_SHORT).show()
//                    }
//                }
                else -> {
                    if (message.startsWith("max_value")) {
                        val parts = message.split(" ")
                        if (parts.size == 2) {
                            val receivedValue = parts[1].toIntOrNull() ?: 0
                            if (receivedValue > maxValue) {
                                maxValue = receivedValue
                                saveMaxValue(maxValue) // Сохранение, если нужно
                                Toast.makeText(context, "Новое максимальное значение: $maxValue", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
//                    // Предыдущая логика обработки простых чисел
//                    if (message.matches(Regex("\\d+"))) {
//                        val receivedValue = message.toInt()
//                        if (receivedValue > maxValue) {
//                            maxValue = receivedValue
//                            saveMaxValue(maxValue)
//                            Toast.makeText(context, "Новое максимальное значение: $maxValue", Toast.LENGTH_SHORT).show()
//                        }
//                    }
                    binding.maxValue.text = message
                }
            }
        }
    }
    private fun saveMaxValue(value: Int) {
        val preferences = activity?.getSharedPreferences(BluetoothConstants.PREFERENCES, Context.MODE_PRIVATE)
        preferences?.edit()?.putInt("max_value", value)?.apply()
    }

    // Получение максимального значения
    private fun getMaxValue(): Int {
        val preferences = activity?.getSharedPreferences(BluetoothConstants.PREFERENCES, Context.MODE_PRIVATE)
        return preferences?.getInt("max_value", 0) ?: 0
    }
}