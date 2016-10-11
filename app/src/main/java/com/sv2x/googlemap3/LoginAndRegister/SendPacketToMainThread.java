package com.sv2x.googlemap3.LoginAndRegister;

import org.json.JSONArray;

/**
 * Created by netlab on 10/11/16.
 */
public interface SendPacketToMainThread {
    void onTextParsed(JSONArray array_of_point, JSONArray array_of_instruc);
}
