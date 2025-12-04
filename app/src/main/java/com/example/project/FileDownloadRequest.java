package com.example.project;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileDownloadRequest extends Request<File> {
    private final Response.Listener<File> mListener;
    private final File mDestinationFile;

    public FileDownloadRequest(String url, File destinationFile, Response.Listener<File> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        mDestinationFile = destinationFile;
        mListener = listener;
        setShouldCache(false); // Do not cache file downloads
    }

    @Override
    protected Response<File> parseNetworkResponse(NetworkResponse response) {
        try {
            // Write the byte data to the destination file
            byte[] data = response.data;
            FileOutputStream fos = new FileOutputStream(mDestinationFile);
            fos.write(data);
            fos.close();

            // Return success with the file object
            return Response.success(mDestinationFile, HttpHeaderParser.parseCacheHeaders(response));
        } catch (IOException e) {
            return Response.error(new com.android.volley.ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(File response) {
        mListener.onResponse(response);
    }
}
