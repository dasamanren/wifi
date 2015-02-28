package com.land.wifi;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.EncodingUtils;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private ListView lvWifi;
	private String url = "/data/misc/wifi/wpa_supplicant.conf";
	private boolean isFirst;
	private List<Network> mNetworks = new ArrayList<Network>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		isFirst = true;
		lvWifi = (ListView) findViewById(R.id.lvWifi);
		if (isRoot()) {
			readFileData(url);
		} else {
			Toast.makeText(this, getString(R.string.no_root), Toast.LENGTH_SHORT).show();
		}
	}

	public boolean isRoot() {
		boolean root = false;
		try {
			if ((!new File("/system/bin/su").exists())
					&& (!new File("/system/xbin/su").exists())) {
				root = false;
			} else {
				root = true;
			}
		} catch (Exception e) {
		}
		return root;
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	Process process = null;

	private void getRoot() {
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes("chmod 775 /data/misc \n");
			os.writeBytes("chmod 775 /data/misc/wifi \n");
			os.writeBytes("chmod 774 /data/misc/wifi/wpa_supplicant.conf \n");
			os.writeBytes("exit\n");
			os.flush();
			try {
				process.waitFor();
				readFileData(url);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			 Toast.makeText(this,getString(R.string.root_fail) , Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private String readFileData(String fileName) {
		String res = "";
		try {
			FileInputStream fin = new FileInputStream(new File(url));
			int length = fin.available();
			byte[] buffer = new byte[length];
			fin.read(buffer);
			res = EncodingUtils.getString(buffer, "utf-8");
			fin.close();
			String[] networks = res.split("network=");
			if (networks != null && networks.length > 0) {
				for (String s : networks) {
					Network network = new Network();
					if (s.contains("ssid")) {
						String[] ssids = s.split("ssid=");
						network.ssid = ssids[1].substring(0,
								ssids[1].indexOf("\n"));
						if (s.contains("psk")) {
							String[] psks = s.split("psk=");
							network.psk = psks[1].substring(0,
									psks[1].indexOf("\n"));
						} else {
							network.psk = "无密码";
						}
						mNetworks.add(network);
						lvWifi.setAdapter(new MyAdapter());
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			if (isFirst) {
				isFirst = false;
				getRoot();
			}
		}
		return res;
	}
	
	public class MyAdapter extends BaseAdapter{
		
		@Override
		public int getCount() {
			return mNetworks.size();
		}

		@Override
		public Object getItem(int position) {
			return mNetworks.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (convertView==null) {
				viewHolder = new ViewHolder();
				convertView = getLayoutInflater().inflate(R.layout.item_main, null);
				viewHolder.tvSsid = (TextView) convertView.findViewById(R.id.tvSsid);
				viewHolder.tvPsk = (TextView) convertView.findViewById(R.id.tvPsk);
				convertView.setTag(viewHolder);
			}else{
				viewHolder = (ViewHolder) convertView.getTag();
			}
			viewHolder.tvSsid.setText(mNetworks.get(position).ssid);
			viewHolder.tvPsk.setText(mNetworks.get(position).psk);
			return convertView;
		}
		
		class ViewHolder{
			public TextView tvSsid;
			public TextView tvPsk;
		}
	}
	
}
