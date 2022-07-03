package com.example.mobilegisundlbsosm;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class RequestPostTask{
    private HttpURLConnection client = null;
    private MapView map = null;
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
    private StringBuilder response = null;


    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    public void PostData(GeoPoint startPoint, MapView mapview){
        wfs_data_t = String.format(Locale.ROOT,WFS_FORMAT_STRING,startPoint.getLongitude(), startPoint.getLatitude());
        map = mapview;

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
            outputPost.write(wfs_data_t.getBytes());
            outputPost.flush();
            outputPost.close();

            // Read XML
            InputStream inputStream = client.getInputStream();
            byte[] res = new byte[2048];
            int i;
            response = new StringBuilder();
            while ((i = inputStream.read(res)) != -1) {
                String input = new String(res, 0, i);
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

    public String getResponse(){

        DocumentBuilder builder;
        StringBuilder result = new StringBuilder();
        String waypoint_id ="";
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            InputSource src = new InputSource();
            src.setCharacterStream(new StringReader(String.valueOf(response)));

            Document doc = builder.parse(src);
            waypoint_id = doc.getElementsByTagName("wfs:InsertResult").item(0).getFirstChild().getAttributes().getNamedItem("fid").getNodeValue();
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }

        result.append("Insert result: ");
        result.append(waypoint_id);
        Log.d("wfs_result", String.valueOf(result));

        return String.valueOf(result);
    }


}
