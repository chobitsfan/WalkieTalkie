package com.gmail.mrchobits.walkietalkie;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SerialInputOutputManager.Listener {
    UsbSerialPort port = null;
    SerialInputOutputManager usbIoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((TextView)findViewById(R.id.inc_msgs)).setMovementMethod(new ScrollingMovementMethod());
        detectMyDevice();
    }

    @Override
    protected void onDestroy() {
        if (port != null) {
            try {
                port.close();
            } catch (IOException e) {
                Log.d("chobits", e.getMessage());
            }
        }
        super.onDestroy();
    }

    public void onSendMsgBtn(View view) {
        TextView tv = findViewById(R.id.out_msg);
        String out_msg = tv.getText().toString();
        if (out_msg.isEmpty()) {
            Toast.makeText(this, "empty message", Toast.LENGTH_SHORT).show();
            return;
        }
        if (port == null) {
            Toast.makeText(this, "no serial port", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            port.write(out_msg.getBytes(), 1000);
        } catch (IOException e) {
            Log.d("chobits", e.getMessage());
        }
    }

    void detectMyDevice() {
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager)getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            Log.d("chobits", "need usb permission");
            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            PendingIntent p_intent = PendingIntent.getBroadcast(this, 0, new Intent("com.example.simplegcs.USB_PERMISSION"), PendingIntent.FLAG_IMMUTABLE);
            manager.requestPermission(driver.getDevice(), p_intent);
            return;
        }

        port = driver.getPorts().get(0); // Most devices have just one port (port 0)
        try {
            port.open(connection);
            port.setParameters(57600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        usbIoManager = new SerialInputOutputManager(port, this);
        usbIoManager.start();
    }

    @Override
    public void onNewData(byte[] data) {
        runOnUiThread(() -> {
            TextView tv = findViewById(R.id.inc_msgs);
            tv.append(new String(data) + "\n");
        });
    }

    @Override
    public void onRunError(Exception e) {

    }
}