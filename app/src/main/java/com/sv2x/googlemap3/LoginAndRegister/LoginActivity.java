package com.sv2x.googlemap3.LoginAndRegister;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

public class LoginActivity extends AppCompatActivity {
    EditText ET_PHONE,ET_PASS;
    String login_phone,login_pass;
    Boolean correct_login=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ET_PHONE = (EditText)findViewById(R.id.etPhonenumber);
        ET_PASS = (EditText)findViewById(R.id.etPassword);

    }
    public void userReg(View view)
    {

        Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();

    }

    /*new AlertDialog.Builder(getApplicationContext())
            .setTitle("Do You Want To Exit From Coposto?")
    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            int pid = android.os.Process.myPid();
            android.os.Process.killProcess(pid);
        }
    })
            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            // do nothing
        }
    })
            .setIcon(android.R.drawable.ic_dialog_alert)
    .show();*/
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
        new AlertDialog.Builder(LoginActivity.this)
                .setTitle("Close App?")
                .setMessage("Do you want to exit?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_HOME);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    public void userLogin(View view)
    {
        login_phone = ET_PHONE.getText().toString();
        login_pass = ET_PASS.getText().toString();
        String method = "login";
        BackgroundTask backgroundTask = new BackgroundTask(this);
        backgroundTask.execute(method, login_phone, login_pass);
        if (correct_login) {
            finish();
        }
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
            String login_url = "http://114.70.9.118/P2P/login.php";
            String method = params[0];
            if (method == "login") {
                String login_phone = params[1];
                String login_pass = params[2];
                try {
                    URL url = new URL(login_url);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);

                    OutputStream outputStream = httpURLConnection.getOutputStream();
                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                    String data = URLEncoder.encode("login_phone", "UTF-8") + "=" + URLEncoder.encode(login_phone, "UTF-8") + "&" +
                            URLEncoder.encode("login_password", "UTF-8") + "=" + URLEncoder.encode(login_pass, "UTF-8");
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
            int login_success = result.indexOf("Success");
            if (login_success >= 0) {
                int name_start_index = result.indexOf("<")+1;
                int name_end_index = result.indexOf(">");
                String USER_NAME = result.substring(name_start_index,name_end_index);
                Toast.makeText(ctx, "Login Successful", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("arg1", USER_NAME);
                intent.putExtra("arg2", login_phone);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                new AlertDialog.Builder(LoginActivity.this)
                        .setTitle("Fail To Login")
                        .setMessage("The password or phone number you entered is incorrect. Please try again.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }

        }
    }

}


