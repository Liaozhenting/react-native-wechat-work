
package com.xinpure.wechatwork;

import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import com.tencent.wework.api.IWWAPI;
import com.tencent.wework.api.IWWAPIEventHandler;
import com.tencent.wework.api.WWAPIFactory;
import com.tencent.wework.api.model.BaseMessage;
import com.tencent.wework.api.model.WWAuthMessage;
import com.tencent.wework.api.model.WWMediaConversation;
import com.tencent.wework.api.model.WWMediaFile;
import com.tencent.wework.api.model.WWMediaImage;
import com.tencent.wework.api.model.WWMediaLink;
import com.tencent.wework.api.model.WWMediaMergedConvs;
import com.tencent.wework.api.model.WWMediaMessage;
import com.tencent.wework.api.model.WWMediaText;
import com.tencent.wework.api.model.WWMediaVideo;

public class RNWeChatWorkModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  private final String WeChatWorkEventName = "EventWeChatWork";
  private DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;

  private IWWAPI iwwapi;
  private String APPID = "";
  private String AGENTID = "";
  private String SCHEMA = "";

  private final static String NOT_REGISTERED = "registerApp required.";
  private final static String INVOKE_FAILED = "WeChat API invoke returns false.";
  private final static String INVALID_ARGUMENT = "invalid argument.";

  // 缩略图大小 kb
  private final static int THUMB_SIZE = 32;

  private static byte[] bitmapTopBytes(Bitmap bitmap) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
    bitmap.recycle();
    return baos.toByteArray();
  }

  private static byte[] bitmapResizeGetBytes(Bitmap image, int size) {
    // little-snow-fox 2019.10.20
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    // 质量压缩方法，这里100表示第一次不压缩，把压缩后的数据缓存到 baos
    image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
    int options = 100;
    // 循环判断压缩后依然大于 32kb 则继续压缩
    while (baos.toByteArray().length / 1024 > size) {
      // 重置baos即清空baos
      baos.reset();
      if (options > 10) {
        options -= 8;
      } else {
        return bitmapResizeGetBytes(Bitmap.createScaledBitmap(image, 280, image.getHeight() / image.getWidth() * 280, true), size);
      }
      // 这里压缩options%，把压缩后的数据存放到baos中
      image.compress(Bitmap.CompressFormat.JPEG, options, baos);
    }
    return baos.toByteArray();
  }

  public RNWeChatWorkModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "WeChatWork";
  }

  @ReactMethod
  public void registerApp(String schema, String appId, String agentId, Callback callback) {
    SCHEMA = schema;
    APPID = appId;
    AGENTID = agentId;

    iwwapi = WWAPIFactory.createWWAPI(this.reactContext);

    eventEmitter = reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);

    callback.invoke(null, iwwapi.registerApp(this.SCHEMA));
  }

  @ReactMethod
  public void isAppInstalled(Callback callback) {
    if (iwwapi == null) {
      callback.invoke(NOT_REGISTERED);
      return;
    }
    callback.invoke(null, iwwapi.isWWAppInstalled());
  }

  @ReactMethod
  public void isAppSupportApi(Callback callback) {
    if (iwwapi == null) {
      callback.invoke(NOT_REGISTERED);
      return;
    }
    callback.invoke(null, iwwapi.isWWAppSupportAPI());
  }

  @ReactMethod
  public void getApiVersion(Callback callback) {
    if (iwwapi == null) {
      callback.invoke(NOT_REGISTERED);
      return;
    }
    callback.invoke(null, iwwapi.getWWAppSupportAPI());
  }

  @ReactMethod
  public void getAppInstallUrl(Callback callback) {
    if (iwwapi == null) {
      callback.invoke(NOT_REGISTERED);
      return;
    }
    callback.invoke(null, "");
  }

  @ReactMethod
  public void openApp(Callback callback) {
    if (iwwapi == null) {
      callback.invoke(NOT_REGISTERED);
      return;
    }
    callback.invoke(null, iwwapi.openWWApp());
  }

  @ReactMethod
  public void SSOAuth(String state) {
    this.SSO(state);
  }

  @ReactMethod
  public void shareLinkAttachment(String title, String summary, String url) {
    if (iwwapi == null) {
      return;
    }
    WWMediaLink link = new WWMediaLink();
    link.description = summary;
    link.webpageUrl = url;
    link.title = title;
    link.appId = APPID;
    link.agentId = AGENTID;
    iwwapi.sendMessage(link);
  }

  @ReactMethod
  public void shareImage(String url) {
    FileInputStream fs = null;
    try{
      if (iwwapi == null) {
        return;
      }

      if (url.indexOf("file://") > -1) {
        url = url.substring(7);
      }

      fs = new FileInputStream(url);
      Bitmap bmp  = BitmapFactory.decodeStream(fs);
      WWMediaImage img = new WWMediaImage();
      img.transaction = "img";
      img.thumbData = bitmapResizeGetBytes(bmp, THUMB_SIZE);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      // 质量压缩方法，这里100表示第一次不压缩，把压缩后的数据缓存到 baos
      bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
      img.fileData = baos.toByteArray();

      bmp.recycle();
      img.appId = APPID;
      img.agentId = AGENTID;
      iwwapi.sendMessage(img);
    } catch (FileNotFoundException err){
      err.printStackTrace();
    }

  }

  public void SSO(String state) {
    if (iwwapi == null) {
      return;
    }
    final WWAuthMessage.Req req = new WWAuthMessage.Req();
    req.sch = SCHEMA;
    req.appId = APPID;
    req.agentId = AGENTID;
    req.state = state;

    iwwapi.sendMessage(req, new IWWAPIEventHandler() {
      @Override
      public void handleResp(BaseMessage resp) {
        WritableMap map = Arguments.createMap();

        if (resp instanceof WWAuthMessage.Resp) {
          WWAuthMessage.Resp rsp = (WWAuthMessage.Resp) resp;

          map.putInt("errCode", rsp.errCode);
          map.putString("code", rsp.code);
          map.putString("state", rsp.state);
          map.putString("type", "SSOAuth.Resp");

          if (rsp.errCode == WWAuthMessage.ERR_CANCEL) {
            map.putString("errStr", "SSOAuth Cancel");
          }else if (rsp.errCode == WWAuthMessage.ERR_FAIL) {
            map.putString("errStr", "SSOAuth Failed");
          } else if (rsp.errCode == WWAuthMessage.ERR_OK) {
            map.putString("errStr", "SSOAuth OK");
          }
        }

        eventEmitter.emit(WeChatWorkEventName, map);
      }
    });
  }

}
