package com.example.mobilegisundlbsosm;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.osmdroid.util.GeoPoint;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class RequestPostTask{
    private HttpURLConnection client = null;
    final private String uri = "http://193.196.36.78:8080"+
            "/geoserver/MobileGIS/ows?SERVICE=WFS";
    final private String WFS_FORMAT_STRING = "<wfs:Transaction service=\"WFS\" version=\"1.0.0\"" +
            " xmlns:MobileGIS=\"http://193.196.36.78\"" +
            " xmlns:ogc=\"http://www.opengis.net/ogc\"" +
            " xmlns:wfs=\"http://www.opengis.net/wfs\">" +
            "<wfs:Insert>" +
            "<waypoints>" +
            "<groupname>" +
            "Android Group" +
            "</groupname>" +
            "<geom>" +
            "<ogc:Point srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">" +
            "<ogc:coordinates>" +
            "%f,%f" +
            "</ogc:coordinates>" +
            "</ogc:Point>" +
            "</geom>" +
            "</waypoints>" +
            "</wfs:Insert>" +
            "</wfs:Transaction>";
    private String wfs_data_t = null;


    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    public void PostData(GeoPoint startPoint){
        wfs_data_t = String.format(Locale.ROOT,WFS_FORMAT_STRING,startPoint.getLongitude(), startPoint.getLatitude());

        try {
            Log.d("wfs_data", wfs_data_t);
            client = (HttpURLConnection) new URL(uri).openConnection();
            client.setRequestMethod("POST");
            // Set DoOutput to true if you want to use URLConnection for output.
            client.setDoOutput(true);
            client.setChunkedStreamingMode(0);
            // Set timeout as per needs
            client.setConnectTimeout(20000);
            client.setReadTimeout(20000);

            // Set Headers
            client.setRequestProperty("Content-Type", "text/xml");

            OutputStream outputPost = new BufferedOutputStream(client.getOutputStream());
            outputPost.write(wfs_data_t.getBytes("UTF-8"));
            outputPost.flush();
            outputPost.close();

            // Read XML
            InputStream inputStream = client.getInputStream();
            byte[] res = new byte[2048];
            int i;
            StringBuilder response = new StringBuilder();
            while ((i = inputStream.read(res)) != -1) {
                response.append(new String(res, 0, i));
            }
            inputStream.close();
            Log.d("wfs_response", String.valueOf(response));
        } catch (Exception e) {
            Log.e("WFS-Post", String.valueOf(e));
        } finally {
            client.disconnect();
        }
    }


}
