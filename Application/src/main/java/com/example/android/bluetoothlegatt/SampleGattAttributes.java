/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String STREAMDATA_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String STREAMDATAD_VALUE_UUID ="0000fff1-0000-1000-8000-00805f9b34fb";
    public static String STREAMDATAD_D0_UUID ="0000fff4-0000-1000-8000-00805f9b34fb";
    public static String STREAMDATAD_D1_UUID ="0000fff5-0000-1000-8000-00805f9b34fb";
    public static String STREAMDATAD_D2_UUID ="0000fff6-0000-1000-8000-00805f9b34fb";
    public static String STREAMDATAD_D3_UUID ="0000fff7-0000-1000-8000-00805f9b34fb";
    public static String STREAMDATAD_D4_UUID ="0000fff3-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put("0000FFF0-0000-1000-8000-00805F9B34FB", "Streamdatad_service");
        // Sample Characteristics.
        attributes.put(STREAMDATA_SERVICE, "DA14580-Service");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put(STREAMDATAD_VALUE_UUID, "Stream_enable_value");

        attributes.put(STREAMDATAD_D0_UUID, "D0_Word_Data");
        attributes.put(STREAMDATAD_D1_UUID, "D1_Erase_Flash");
        attributes.put(STREAMDATAD_D2_UUID, "D2_Renew");
        attributes.put(STREAMDATAD_D3_UUID, "D3_character_type");
        attributes.put(STREAMDATAD_D4_UUID, "D4_animate_type");


    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
