package e.ics_vds;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class UserPrefs {
    private SharedPreferences sPrefs;
    private SharedPreferences.Editor editor;

    public UserPrefs(Context context){
        sPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sPrefs.edit();
    }

    public void seMqttClientId(String clientId){
        editor.putString("clientId", clientId);
        editor.apply();
    }
    public String getMqttClientId(){
        return sPrefs.getString("clientId", null);
    }

}
