/*
* This is the source code of iGap for Android
* It is licensed under GNU AGPL v3.0
* You should have received a copy of the license in this archive (see LICENSE).
* Copyright © 2017 , iGap - www.iGap.net
* iGap Messenger | Free, Fast and Secure instant messaging application
* The idea of the RooyeKhat Media Company - www.RooyeKhat.co
* All rights reserved.
*/

package net.iGap.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.afollestad.materialdialogs.MaterialDialog;
import io.realm.Realm;
import io.realm.RealmResults;
import java.io.File;
import java.io.IOException;
import net.iGap.G;
import net.iGap.R;
import net.iGap.helper.HelperAvatar;
import net.iGap.helper.HelperCalander;
import net.iGap.helper.HelperPermision;
import net.iGap.helper.HelperUploadFile;
import net.iGap.helper.ImageHelper;
import net.iGap.interfaces.OnAvatarAdd;
import net.iGap.interfaces.OnGetPermission;
import net.iGap.interfaces.OnUserAvatarResponse;
import net.iGap.interfaces.OnUserInfoResponse;
import net.iGap.interfaces.OnUserProfileSetNickNameResponse;
import net.iGap.module.AndroidUtils;
import net.iGap.module.AttachFile;
import net.iGap.module.EditTextAdjustPan;
import net.iGap.module.FileUploadStructure;
import net.iGap.module.IntentRequests;
import net.iGap.proto.ProtoGlobal;
import net.iGap.proto.ProtoResponse;
import net.iGap.realm.RealmAvatar;
import net.iGap.realm.RealmAvatarFields;
import net.iGap.realm.RealmUserInfo;
import net.iGap.request.RequestUserAvatarAdd;
import net.iGap.request.RequestUserInfo;
import net.iGap.request.RequestUserProfileSetNickname;

import static net.iGap.G.context;
import static net.iGap.module.AttachFile.request_code_image_from_gallery_single_select;

public class ActivityProfile extends ActivityEnhanced implements OnUserAvatarResponse {

    public final static String ARG_USER_ID = "arg_user_id";
    public static boolean IsDeleteFile;
    public static Bitmap decodeBitmapProfile = null;
    private TextView txtTitle, txtTitlInformation, txtDesc, txtAddPhoto;
    private Button btnLetsGo;
    private net.iGap.module.CircleImageView btnSetImage;
    private EditTextAdjustPan edtNikName;
    private Uri uriIntent;
    private String pathImageUser;
    private File pathImageFromCamera = new File(G.imageFile.toString() + "_" + 0 + ".jpg");
    private int idAvatar;
    private int lastUploadedAvatarId;
    private ProgressBar prgWait;
    private boolean existAvatar = false;
    private Typeface titleTypeface;


    @Override protected void onResume() {
        super.onResume();
        G.onUserAvatarResponse = this;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        delete();
        txtTitlInformation = (TextView) findViewById(R.id.pu_txt_title_information);

        txtDesc = (TextView) findViewById(R.id.pu_txt_title_desc);

        txtAddPhoto = (TextView) findViewById(R.id.pu_txt_addPhoto);
        prgWait = (ProgressBar) findViewById(R.id.prg);

        findViewById(R.id.ap_ll_toolbar).setBackgroundColor(Color.parseColor(G.appBarColor));

        txtTitle = (TextView) findViewById(R.id.pu_titleToolbar);

        if (!HelperCalander.isLanguagePersian) {
            titleTypeface = Typeface.createFromAsset(getAssets(), "fonts/neuropolitical.ttf");
        } else {
            titleTypeface = Typeface.createFromAsset(getAssets(), "fonts/IRANSansMobile.ttf");
        }
        txtTitle.setTypeface(titleTypeface);

        final View lineEditText = findViewById(R.id.pu_line_below_editText);
        btnSetImage = (net.iGap.module.CircleImageView) findViewById(R.id.pu_profile_circle_image);

        AndroidUtils.setBackgroundShapeColor(btnSetImage, Color.parseColor(G.appBarColor));

        btnSetImage.setOnClickListener(new View.OnClickListener() { // button for set image
            @Override public void onClick(View view) {
                if (!existAvatar) {

                    startDialog();
                }
            }
        });

        final TextInputLayout txtInputNickName = (TextInputLayout) findViewById(R.id.pu_txtInput_nikeName);
        //        txtInputNickName.setHint("Nickname");

        edtNikName = (EditTextAdjustPan) findViewById(R.id.pu_edt_nikeName); // edit Text for NikName

        btnLetsGo = (Button) findViewById(R.id.pu_btn_letsGo);
        btnLetsGo.setBackgroundColor(Color.parseColor(G.appBarColor));

        edtNikName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                txtInputNickName.setErrorEnabled(true);
                txtInputNickName.setError("");
                txtInputNickName.setHintTextAppearance(R.style.remove_error_appearance);
                edtNikName.setTextColor(getResources().getColor(R.color.border_editText));
                lineEditText.setBackgroundColor(getResources().getColor(android.R.color.black));
            }

            @Override public void afterTextChanged(Editable editable) {

            }
        });
        btnLetsGo.setOnClickListener(new View.OnClickListener() { // button for save data and go to next page
            @Override public void onClick(View view) {

                Realm realm = Realm.getDefaultInstance();
                final String nickName = edtNikName.getText().toString();

                if (!nickName.equals("")) {

                    showProgressBar();
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override public void execute(Realm realm) {
                            setNickName();
                        }
                    });
                    realm.close();
                } else {
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN) @Override public void run() {

                            txtInputNickName.setErrorEnabled(true);
                            txtInputNickName.setError(getResources().getString(R.string.Toast_Write_NickName));
                            txtInputNickName.setHintTextAppearance(R.style.error_appearance);
                            edtNikName.setTextColor(getResources().getColor(R.color.red));
                            lineEditText.setBackgroundColor(getResources().getColor(R.color.red));
                        }
                    });
                }
            }
        });
    }

    private void setNickName() {

        G.onUserProfileSetNickNameResponse = new OnUserProfileSetNickNameResponse() {
            @Override public void onUserProfileNickNameResponse(final String nickName, ProtoResponse.Response response) {
                getUserInfo();
            }

            @Override public void onUserProfileNickNameError(int majorCode, int minorCode) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        hideProgressBar();
                    }
                });
            }

            @Override public void onUserProfileNickNameTimeOut() {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        hideProgressBar();
                    }
                });
            }
        };

        new RequestUserProfileSetNickname().userProfileNickName(edtNikName.getText().toString());
    }

    private void getUserInfo() {

        G.onUserInfoResponse = new OnUserInfoResponse() {
            @Override public void onUserInfo(final ProtoGlobal.RegisteredUser user, String identity) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                        Realm realm = Realm.getDefaultInstance();
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override public void execute(Realm realm) {
                                RealmUserInfo realmUserInfo = realm.where(RealmUserInfo.class).findFirst();
                                realmUserInfo.getUserInfo().setDisplayName(user.getDisplayName());
                                realmUserInfo.getUserInfo().setInitials(user.getInitials());
                                realmUserInfo.getUserInfo().setColor(user.getColor());

                                final long userId = realmUserInfo.getUserId();

                                runOnUiThread(new Runnable() {
                                    @Override public void run() {
                                        G.onUserInfoResponse = null;
                                        hideProgressBar();
                                        Intent intent = new Intent(context, ActivityMain.class);
                                        intent.putExtra(ActivityProfile.ARG_USER_ID, userId);
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                            }
                        });
                        realm.close();
                    }
                });
            }

            @Override public void onUserInfoTimeOut() {

                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        hideProgressBar();
                    }
                });
            }

            @Override public void onUserInfoError(int majorCode, int minorCode) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        hideProgressBar();
                    }
                });
            }
        };

        Realm realm = Realm.getDefaultInstance();
        RealmUserInfo realmUserInfo = realm.where(RealmUserInfo.class).findFirst();
        new RequestUserInfo().userInfo(realmUserInfo.getUserId());
        realm.close();
    }

    public void useCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                new AttachFile(ActivityProfile.this).dispatchTakePictureIntent();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                new AttachFile(ActivityProfile.this).requestTakePicture();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //======================================================================================================dialog for choose image

    public void useGallery() {
        //        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        //        startActivityForResult(intent, IntentRequests.REQ_GALLERY);

        try {
            HelperPermision.getStoragePermision(context, new OnGetPermission() {
                @Override public void Allow() {
                    try {
                        new AttachFile(ActivityProfile.this).requestOpenGalleryForImageSingleSelect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override public void deny() {

                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startDialog() {

        new MaterialDialog.Builder(this).title(getResources().getString(R.string.choose_picture))
            .negativeText(getResources().getString(R.string.B_cancel))
            .items(R.array.profile)
            .itemsCallback(new MaterialDialog.ListCallback() {
                               @Override public void onSelection(final MaterialDialog dialog, View view, int which, CharSequence text) {

                                   switch (which) {
                                       case 0: {
                                           useGallery();
                                           dialog.dismiss();
                                           break;
                                       }
                                       case 1: {
                                           if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                                               try {

                                                   HelperPermision.getStoragePermision(ActivityProfile.this, new OnGetPermission() {
                                                       @Override public void Allow() throws IOException {
                                                           HelperPermision.getCameraPermission(ActivityProfile.this, new OnGetPermission() {
                                                               @Override public void Allow() {
                                                                   // this dialog show 2 way for choose image : gallery and camera
                                                                   dialog.dismiss();
                                                                   useCamera();
                                                               }

                                                               @Override public void deny() {

                                                               }
                                                           });
                                                       }

                                                       @Override public void deny() {

                                                       }
                                                   });
                                               } catch (IOException e) {
                                                   e.printStackTrace();
                                               }
                                           } else {
                                               final Snackbar snack = Snackbar.make(findViewById(android.R.id.content), getResources().getString(R.string.please_check_your_camera), Snackbar.LENGTH_LONG);

                                               snack.setAction(getString(R.string.cancel), new View.OnClickListener() {
                                                   @Override public void onClick(View view) {
                                                       snack.dismiss();
                                                   }
                                               });
                                               snack.show();
                                           }
                                           break;
                                       }
                                   }
                               }
                           }

            )
            .

                show();
    }

    //======================================================================================================result from camera , gallery and crop
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AttachFile.request_code_TAKE_PICTURE && resultCode == RESULT_OK) {// result for camera

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                Intent intent = new Intent(ActivityProfile.this, ActivityCrop.class);
                ImageHelper.correctRotateImage(AttachFile.mCurrentPhotoPath, true);
                intent.putExtra("IMAGE_CAMERA", AttachFile.mCurrentPhotoPath);
                intent.putExtra("TYPE", "camera");
                intent.putExtra("PAGE", "profile");
                intent.putExtra("ID", (int) getIntent().getLongExtra(ARG_USER_ID, -1));
                startActivityForResult(intent, IntentRequests.REQ_CROP);
            } else {
                Intent intent = new Intent(ActivityProfile.this, ActivityCrop.class);
                ImageHelper.correctRotateImage(AttachFile.imagePath, true);
                intent.putExtra("IMAGE_CAMERA", AttachFile.imagePath);
                intent.putExtra("TYPE", "camera");
                intent.putExtra("PAGE", "profile");
                intent.putExtra("ID", (int) getIntent().getLongExtra(ARG_USER_ID, -1));
                startActivityForResult(intent, IntentRequests.REQ_CROP);
            }
        } else if (requestCode == request_code_image_from_gallery_single_select && resultCode == RESULT_OK) {// result for gallery
            if (data != null) {
                Intent intent = new Intent(ActivityProfile.this, ActivityCrop.class);
                if (data.getData() == null) {
                    return;
                }
                intent.putExtra("IMAGE_CAMERA", AttachFile.getFilePathFromUri(data.getData()));
                intent.putExtra("TYPE", "gallery");
                intent.putExtra("PAGE", "profile");
                intent.putExtra("ID", (int) getIntent().getLongExtra(ARG_USER_ID, -1));
                startActivityForResult(intent, IntentRequests.REQ_CROP);
            }
        } else if (requestCode == IntentRequests.REQ_CROP && resultCode == RESULT_OK) {
            if (data != null) {
                pathImageUser = data.getData().toString();
            }

            lastUploadedAvatarId = idAvatar + 1;

            showProgressBar();
            HelperUploadFile.startUploadTaskAvatar(pathImageUser, lastUploadedAvatarId, new HelperUploadFile.UpdateListener() {
                @Override public void OnProgress(int progress, FileUploadStructure struct) {
                    if (progress < 100) {
                        prgWait.setProgress(progress);
                    } else {
                        new RequestUserAvatarAdd().userAddAvatar(struct.token);
                    }
                }

                @Override public void OnError() {
                    hideProgressBar();
                }
            });
        }
    }

    private void setImage(String path) {
        if (path != null) {

            G.imageLoader.displayImage(AndroidUtils.suitablePath(path), btnSetImage);
            btnSetImage.setPadding(0, 0, 0, 0);
            //Bitmap bitmap = BitmapFactory.decodeFile(path);
            //btnSetImage.setImageBitmap(bitmap);
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (pathImageFromCamera.exists()) {
                pathImageFromCamera.delete();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void delete() {
        Realm realm = Realm.getDefaultInstance();
        RealmUserInfo realmUserInfo = realm.where(RealmUserInfo.class).findFirst();
        if (realmUserInfo != null) {
            final long userId = realm.where(RealmUserInfo.class).findFirst().getUserId();
            final RealmResults<RealmAvatar> realmAvatars = realm.where(RealmAvatar.class).equalTo(RealmAvatarFields.OWNER_ID, userId).findAll();
            if (!realmAvatars.isEmpty()) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override public void execute(Realm realm) {
                        realmAvatars.deleteAllFromRealm();
                    }
                });
            }
        }
        realm.close();
    }

    @Override public void onAvatarAdd(final ProtoGlobal.Avatar avatar) {

        Realm realm = Realm.getDefaultInstance();
        long userId = realm.where(RealmUserInfo.class).findFirst().getUserId();
        HelperAvatar.avatarAdd(userId, pathImageUser, avatar, new OnAvatarAdd() {
            @Override public void onAvatarAdd(final String avatarPath) {

                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        existAvatar = true;
                        hideProgressBar();
                        setImage(avatarPath);
                    }
                });
            }
        });
    }

    @Override public void onAvatarAddTimeOut() {
        hideProgressBar();
    }

    @Override public void onAvatarError() {
        hideProgressBar();
    }

    //***Show And Hide Progress

    private void showProgressBar() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (prgWait != null) {
                    prgWait.setVisibility(View.VISIBLE);
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            }
        });
    }

    private void hideProgressBar() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (prgWait != null) {
                    prgWait.setVisibility(View.GONE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                }
            }
        });
    }

    @Override public void onBackPressed() {
        super.onBackPressed();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            finishAffinity();
        } else {
            ActivityCompat.finishAffinity(this);
        }
    }
}
