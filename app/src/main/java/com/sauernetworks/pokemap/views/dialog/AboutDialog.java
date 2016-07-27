package com.sauernetworks.pokemap.views.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.sauernetworks.pokemap.R;

public class AboutDialog extends Dialog {
	private static Context mContext = null;

    public AboutDialog(Context context) {
        super(context);
        mContext = context;
    }


	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.dialog_about);
		
		TextView legal = (TextView)findViewById(R.id.about_legal_text);
		legal.setText(readRawTextFile(R.raw.legal));

		TextView info = (TextView)findViewById(R.id.about_info_text);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            info.setText(Html.fromHtml(readRawTextFile(R.raw.info),Html.FROM_HTML_MODE_LEGACY));
		} else {
            //noinspection deprecation
            info.setText(Html.fromHtml(readRawTextFile(R.raw.info)));
		}

		info.setTextColor(Color.BLACK);
		Linkify.addLinks(info, Linkify.WEB_URLS);
	}

	private String getVersion() {
		try {
			String version = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
			String code = String.valueOf(mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode);
			return version + " build " + code;
		} catch (PackageManager.NameNotFoundException e) {
			return "0";
		}
	}
	
	private String readRawTextFile(int id) {
		InputStream inputStream = mContext.getResources().openRawResource(id);
		InputStreamReader in = new InputStreamReader(inputStream);
		BufferedReader buf = new BufferedReader(in);
		String line;
		StringBuilder text = new StringBuilder();
		try {
			while (( line = buf.readLine()) != null) {
				String version = getVersion();
				line = line.replaceAll("%SW_VERSION%", version);
				text.append(line);
			}
		} catch (IOException e) {
			return null;
		}
		return text.toString();
	}
}