package com.safevoice.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "ScamReport")
public class ScamReportPlugin extends Plugin {

    @PluginMethod
    public void reportNumber(PluginCall call) {
        String number = call.getString("number");
        if (number == null || number.isEmpty()) {
            call.reject("Missing number");
            return;
        }

        ScamBlocklist.getInstance(getContext()).addUserReportedNumber(number);

        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }
}