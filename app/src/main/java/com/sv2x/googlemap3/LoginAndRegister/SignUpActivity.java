package com.sv2x.googlemap3.LoginAndRegister;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.sv2x.googlemap3.MainActivity;
import com.sv2x.googlemap3.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;


public class SignUpActivity extends Activity {
    EditText ET_NAME,ET_PASS,ET_PHONE,ET_EMAIL;
    String Phone_number;
    String User_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        ET_NAME = (EditText)findViewById(R.id.etName);
        ET_PHONE = (EditText) findViewById(R.id.etPhonenumber);
        ET_PASS = (EditText)findViewById(R.id.etPassword);
        ET_EMAIL = (EditText)findViewById(R.id.email_address);


    }
    public void userReg(View view)
    {
        String user_name,user_phone,user_pass,user_email;
        user_name = ET_NAME.getText().toString();
        user_phone = ET_PHONE.getText().toString();
        user_pass =ET_PASS.getText().toString();
        user_email = ET_EMAIL.getText().toString();
        String method = "register";
        Phone_number = user_phone;
        User_name=user_name;
        BackgroundTask backgroundTask = new BackgroundTask(this);
        backgroundTask.execute(method, user_name, user_phone,user_email, user_pass);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 1) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void onBackPressed() {
        Log.d("CDA", "onBackPressed Called");

        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();

    }

    public class BackgroundTask extends AsyncTask<String,Void,String> {
        AlertDialog alertDialog;
        Context ctx;

        BackgroundTask(Context ctx) {
            this.ctx = ctx;
        }

        @Override
        protected void onPreExecute() {
            alertDialog = new AlertDialog.Builder(ctx).create();
            alertDialog.setTitle("Login Information....");
        }

        @Override
        protected String doInBackground(String... params) {
            String reg_url = "http://114.70.9.118/P2P/register.php";
            String method = params[0];
            if (method == "register") {
                String user_name = params[1];
                String user_phone = params[2];
                String user_email = params[3];
                String user_pass = params[4];
                try {
                    URL url = new URL(reg_url);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);

                    //httpURLConnection.setDoInput(true);
                    OutputStream outputStream = httpURLConnection.getOutputStream();
                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                    String data = URLEncoder.encode("user_name", "UTF-8") + "=" + URLEncoder.encode(user_name, "UTF-8") + "&" +
                            URLEncoder.encode("user_phone", "UTF-8") + "=" + URLEncoder.encode(user_phone, "UTF-8") + "&" +
                            URLEncoder.encode("user_email", "UTF-8") + "=" + URLEncoder.encode(user_email, "UTF-8") + "&" +
                            URLEncoder.encode("user_password", "UTF-8") + "=" + URLEncoder.encode(user_pass, "UTF-8");
                    bufferedWriter.write(data);
                    bufferedWriter.flush();
                    bufferedWriter.close();
                    outputStream.close();

                    InputStream inputStream = httpURLConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "iso-8859-1"));
                    String response = "";
                    String line = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        response += line;
                    }
                    bufferedReader.close();
                    inputStream.close();

                    httpURLConnection.disconnect();
                    alertDialog.dismiss();
                    return response;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.equals(" Registration Success...")) {
                Toast.makeText(ctx, result, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                intent.putExtra("arg1", Phone_number);
                intent.putExtra("arg2", User_name);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                alertDialog.setTitle("Fail to register");
                alertDialog.setMessage(result);
                alertDialog.show();
            }

        }
    }

}