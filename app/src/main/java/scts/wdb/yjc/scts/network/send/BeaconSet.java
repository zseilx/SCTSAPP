package scts.wdb.yjc.scts.network.send;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import scts.wdb.yjc.scts.HttpClient;
import scts.wdb.yjc.scts.WebViewMain;
import scts.wdb.yjc.scts.bean.IPSetting;

import static android.content.ContentValues.TAG;

/**
 * Created by JYH on 2017-04-06.
 */

public class BeaconSet extends AsyncTask<String, String, String> {
    private Context mContext;

    private SharedPreferences sp;

    private SharedPreferences.Editor editor;

    public BeaconSet(Context mContext) {
        this.mContext = mContext;
        sp = mContext.getSharedPreferences("test", 0);
    }

    protected  void onPreExcute(){

        super.onPreExecute();
    }
    @Override
    protected String doInBackground(String... params) {
        // bean 안에 있는 ip 셋팅 정보를 꼭 바꾸도록 할 것
        String command = "firstCourse";
        if(params[0].equals("secondSend"))
            command = "secondCourse";

        HttpClient.Builder http = new HttpClient.Builder("POST", IPSetting.getIpAddress() + command);
        http.addOrReplace("CourseVO", params[1]);
        /*
        for(int i=0; i<params.length; i++) {
            http.addOrReplace("" + i,params[i]);
        }
        */

        // HTTP 요청 전송
        HttpClient post = http.create();

        post.request();

        // 응답 상태코드 가져오기
        int statusCode = post.getHttpStatusCode();

        // 응답 본문 가져오기
        String body = post.getBody();

        return body;
    }

    // jsp에서 리턴한 데이터 처리부분
    /**************************** 이부분이 쿠폰 받은 부분이다!!!!!!!!!!!!!!! **************************/

    protected void onPostExecute(String s) {
        Log.d(TAG, "onPostExecute: " + s);
            //String encode = URLEncoder.encode(s, "UTF-8");
            //Log.d(TAG, "onPostExecute: " + "한글 엔코드 결과" + encode);

        // 제이슨 사용시 주의할 점이 getAsString()를 사용하지 않으면 데이터에 "" 가 붙어서 나와서 equals 사용때 이스케이프 문자열로 쌍따운표 붙여줘야됨.
        // 안붙이고 사용할 거면 getAsString() 메소드를 사용해서 ""를 제거해줘야됨
        JsonObject json = new Gson().fromJson(s, JsonObject.class);
        Log.d(TAG, "onPostExecute: " + json.toString());

        // 성공시 처리
        if( json.get("status").getAsString().equals("SUCCESS") ) {
            // 가져온 데이터에서 명령문을 해독함
            String command = json.get("command").getAsString().toString();
            Log.d(TAG, "onSUCCESS: " + command);


            if( json.get("bhf_code") != null) {
                editor = sp.edit();
                String bhf_code = json.get("bhf_code").getAsString().toString();
                Log.d("BeaconSet: ", "bhf_code : " + bhf_code);
                editor.putString("bhf_code", bhf_code);
                editor.commit();
            }


            if( json.get("tile") != null) {
                editor = sp.edit();
                String standingTile = json.get("tile").toString();
                Log.d("BeaconSet: ", "standingTile : " + standingTile);
                editor.putString("standingTile", standingTile);
                editor.commit();

                JsonObject tileJson = (JsonObject) json.get("tile");
                tileDataRecive(tileJson);
            }

            // 돌아온 데이터에 쿠폰이 존재하지 않을 때
            // 해당 명령문은 비콘 데이터 첫번째 전송, 두번째 전송 모두에서 이루어 질 수 있으며
            // 두번째 전송의 경우에는 무조건 emptycoupon에 속하게 되어있음
            // 첫번째 전송의 경우에는 쿠폰이 비어있을 경우에만 가능
            if( command.equals("emptycoupon") ) {
                Log.d(TAG, "onFULL: " + command);
            }

            // 돌아온 데이터에 쿠폰이 있을 때.
            // 해당 명령문은 무조건 비콘 데이터전송의 첫번째 에서만 이루어 질 수 있음
            else if(command.equals("fullcoupon")) {
                Log.d(TAG, "notCOUPON: " + command);
                JsonObject couponJson = (JsonObject) json.get("coupon");
                fullRecive(couponJson);
            }
        }
        // 에러 처리
        else {
            Log.d(TAG, "post:" + json.get("errorCode").getAsString().toString());
        }

    }
    // 쿠폰 받은게 있을 경우에 처리하는 메서드
    protected void fullRecive(JsonObject json) {
        // json data 이런형식으로 날아옴. 뿌려주는건 아직 안함.
        // {"coupon_dscnt":"10%","coupon_begin_de":"1월 1, 2017","coupon_code":2,"coupon_end_de":"12월 30, 2017","coupon_cntnts":"coffee sale","coupon_nm":"coffee man","command":"fullcoupon","status":"SUCCESS"}


        try {
            // 해당 부분에서 json 데이터 처리할 것
            // json 데이터 쿠폰으로 송신
            Log.d(TAG, "fullRecive: " + json);
            ((WebViewMain)mContext).setCoupon(json);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 쿠폰 받은거 없을때 처리하는 부분인데 현재 아무것도 할 게 없어서 안해놧음
    protected void emptyRecive(JsonObject json) {
        // 공백
    }

    protected void tileDataRecive(JsonObject tileJson) {
        // 여기로 타일 데이터 넘어옴
        // 형식은 : {"TILE_CRDNT_X":0,"TILE_CODE":128,"TILE_CRDNT_Y":9,"TILE_NM":"A_0_9","DRW_CODE":4}

        try {
            Log.d(TAG, "tileDataRecive: " + tileJson);
            ((WebViewMain)mContext).setTile(tileJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
