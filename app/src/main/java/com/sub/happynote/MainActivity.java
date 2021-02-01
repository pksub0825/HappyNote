package com.sub.happynote;

import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    FirebaseRemoteConfig remoteConfig;
    long newAppVersion=1;
    long toolbarImgCount=15;
    List<File> toolbarImgList=new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getRemoteConfig();
    }

    private void getRemoteConfig() {
        remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                //.setMinimumFetchIntervalInSeconds(3600)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);

        newAppVersion=remoteConfig.getLong("new_app_version");
        toolbarImgCount=remoteConfig.getLong("toolbar_img_count");

        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        newAppVersion=remoteConfig.getLong("new_app_version");
                        toolbarImgCount=remoteConfig.getLong("toolbar_img_count");
                        checkVersion();
                    }
                });
    }

    private void checkVersion() { //버젼 체크 메서드
            try {
                PackageInfo pi=getPackageManager().getPackageInfo(getPackageName(), 0);
                long appVersion;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    appVersion=pi.getLongVersionCode();
                }else{
                    appVersion=pi.versionCode;
                }
                if(appVersion < newAppVersion){ //앱버젼이 낮으면 업데이트 실행
                    updateDialog();
                    return;
                }
                checkToolbarImages();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
    }

    @Override
    protected void onStop() {
        super.onStop();
        toolbarImgList.clear();
        toolbarImgList=null;
    }

    private void checkToolbarImages() {
        File file=getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/toolbar_images"); //이미지 다운로드 경로 지정
        //경로에 파일이 없으면 만들도록
        if (!file.isDirectory()) {
            file.mkdir();
        }

        toolbarImgList.addAll(new ArrayList<>(Arrays.asList(file.listFiles())));

        if (toolbarImgList.size() < toolbarImgCount) {
            FirebaseStorage storage=FirebaseStorage.getInstance();
            StorageReference storageReference=storage.getReference();
            downloadToolbarImg(storageReference);
        }
    }

    private void downloadToolbarImg(final StorageReference storageReference) {

        if(toolbarImgList==null || toolbarImgList.size()>= toolbarImgCount) return;

        String fileName = "toolbar_"+toolbarImgList.size()+".jpg";
        File fileDir=getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/toolbar_images");
        final File downloadFile=new File(fileDir, fileName);
        StorageReference downloadRef = storageReference
                .child("toolbar_images/"+"toolbar_"+toolbarImgList.size()+".jpg");

        downloadRef.getFile(downloadFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                toolbarImgList.add(downloadFile);
                if (toolbarImgList.size() < toolbarImgCount) {
                    downloadToolbarImg(storageReference);
                }
                Log.e("onSuccess", downloadFile.getName());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }

    private void updateDialog() {
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("업데이트 알림");
        builder.setMessage("최신버전이 등록되었습니다.\n업데이트 하세요")
            .setCancelable(false).setPositiveButton("업데이트", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /*Intent intent=new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.sub.happynote"));
                startActivity(intent);*/
                Toast.makeText(getApplicationContext(), "업데이트 버튼 클릭됨", Toast.LENGTH_SHORT).show();
                dialog.cancel();
            }
        });
        AlertDialog alertDialog=builder.create();
        alertDialog.show();
    }
}