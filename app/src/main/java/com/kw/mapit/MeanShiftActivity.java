package com.kw.mapit;

import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.nhn.android.maps.NMapActivity;
import com.nhn.android.maps.NMapController;
import com.nhn.android.maps.NMapOverlay;
import com.nhn.android.maps.NMapOverlayItem;
import com.nhn.android.maps.NMapView;
import com.nhn.android.maps.maplib.NGeoPoint;
import com.nhn.android.maps.nmapmodel.NMapError;
import com.nhn.android.maps.nmapmodel.NMapPlacemark;
import com.nhn.android.maps.overlay.NMapCircleData;
import com.nhn.android.maps.overlay.NMapCircleStyle;
import com.nhn.android.maps.overlay.NMapPOIdata;
import com.nhn.android.maps.overlay.NMapPOIitem;
import com.nhn.android.maps.overlay.NMapPathData;
import com.nhn.android.maps.overlay.NMapPathLineStyle;
import com.nhn.android.mapviewer.overlay.NMapCalloutCustomOverlay;
import com.nhn.android.mapviewer.overlay.NMapCalloutOverlay;
import com.nhn.android.mapviewer.overlay.NMapOverlayManager;
import com.nhn.android.mapviewer.overlay.NMapPOIdataOverlay;
import com.nhn.android.mapviewer.overlay.NMapPathDataOverlay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MeanShiftActivity extends NMapActivity implements NMapView.OnMapStateChangeListener {
    String myJSON;

    private static final String TAG_RESULTS = "result";
    private static final String TAG_TEXT_NUM = "text_num";
    private static final String TAG_LONGITUDE = "longitude";
    private static final String TAG_LATITUDE = "latitude";

    String textNum;
    String longitude;
    String latitude;

    boolean isInit=false;

    JSONArray location = null;

    // API-KEY
    public static final String API_KEY = "29AIQje_U7muB8tVyofe";  //<---맨위에서 발급받은 본인 ClientID 넣으세요.
    // 네이버 맵 객체
    NMapView mMapView = null;
    // 맵 컨트롤러
    NMapController mMapController = null;
    // 맵을 추가할 레이아웃
    LinearLayout MapContainer;

    private static final String LOG_TAG = "NMapViewer";
    private static final boolean DEBUG = false;

    private NMapViewerResourceProvider mMapViewerResourceProvider;
    private NMapOverlayManager mOverlayManager;

    private NMapPOIdataOverlay mFloatingPOIdataOverlay;
    private NMapPOIitem mFloatingPOIitem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mean_shift);

        String serverURL = "http://" + getString(R.string.ip) + "/selectLocation.php";
        getData(serverURL);

        // 네이버 지도를 넣기 위한 LinearLayout 컴포넌트
        MapContainer = (LinearLayout) findViewById(R.id.MapContainer);

        // 네이버 지도 객체 생성
        mMapView = new NMapView(this);

        // 지도 객체로부터 컨트롤러 추출
        mMapController = mMapView.getMapController();

        // 네이버 지도 객체에 APIKEY 지정
        mMapView.setClientId(API_KEY);
        // 생성된 네이버 지도 객체를 LinearLayout에 추가시킨다.
        MapContainer.addView(mMapView);

        // 지도를 터치할 수 있도록 옵션 활성화f
        mMapView.setClickable(true);
        mMapView.setEnabled(true);
        mMapView.setFocusable(true);
        mMapView.setFocusableInTouchMode(true);
        mMapView.requestFocus();

        // 확대/축소를 위한 줌 컨트롤러 표시 옵션 활성화
        mMapView.setBuiltInZoomControls(true, null);

        // 지도에 대한 상태 변경 이벤트 연결
        mMapView.setOnMapStateChangeListener(this);

        //create resource provider
        mMapViewerResourceProvider = new NMapViewerResourceProvider(this);

        //create overlay manager
        mOverlayManager = new NMapOverlayManager(this, mMapView, mMapViewerResourceProvider);

        // set data provider listener
        super.setMapDataProviderListener(onDataProviderListener);

        //Markers for POI item
        int marker1 = NMapPOIflagType.PIN;

        // set POI data
        NMapPOIdata poiData = new NMapPOIdata(1, mMapViewerResourceProvider);

        poiData.beginPOIdata(1);
        NMapPOIitem item = poiData.addPOIitem(null, "Touch & drag to Move", marker1, 0);
        if (item != null) {
            //initialize location to the center of the map view
            item.setPoint(mMapController.getMapCenter());

            //set floating mode
            item.setFloatingMode(NMapPOIitem.FLOATING_TOUCH | NMapPOIitem.FLOATING_DRAG);

            //show right button on callout
            item.setRightButton(true);

            item.setRightAccessory(true, NMapPOIflagType.CLICKABLE_ARROW);
            mFloatingPOIitem = item;
        }
        poiData.endPOIdata();

        //create POI data overlay
        NMapPOIdataOverlay poiDataOverlay = mOverlayManager.createPOIdataOverlay(poiData, null);

        if (poiDataOverlay != null) {
            poiDataOverlay.setOnFloatingItemChangeListener(onPOIdataFloatingItemChangeListener);

            //set event listener to the overlay
            poiDataOverlay.setOnStateChangeListener(onPOIdataStateChangeListener);

            poiDataOverlay.selectPOIitem(0, false);

            mFloatingPOIdataOverlay = poiDataOverlay;
        }

        //register callout overlay listener to customize it.
        mOverlayManager.setOnCalloutOverlayListener(onCalloutOverlayListener);

    }

    protected   void showLog() {
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            location = jsonObj.getJSONArray(TAG_RESULTS);

            for(int i=0; i<location.length(); i++) {
                JSONObject c = location.getJSONObject(i);
                textNum = c.optString(TAG_TEXT_NUM);
                longitude = c.optString(TAG_LONGITUDE);
                latitude = c.optString(TAG_LATITUDE);

                Log.i(LOG_TAG, "text_num = "+textNum);
                Log.i(LOG_TAG, "longitude = "+longitude);
                Log.i(LOG_TAG, "latitude = "+latitude);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    protected void matchData(){ //데이터를 점에 매칭
        try {
            JSONObject jsonObj = new JSONObject(myJSON);
            location = jsonObj.getJSONArray(TAG_RESULTS);

            for(int i=0; i<location.length(); i++) {
                JSONObject c = location.getJSONObject(i);
                textNum = c.optString(TAG_TEXT_NUM);
                longitude = c.optString(TAG_LONGITUDE);
                latitude = c.optString(TAG_LATITUDE);

                // set path data points
                NMapPathData pathData = new NMapPathData(1);

                //데이터 위치 점 찍어주는 부분
                pathData.initPathData();
                pathData.addPathPoint(Float.parseFloat(longitude), Float.parseFloat(latitude), NMapPathLineStyle.TYPE_SOLID);
                pathData.addPathPoint(Float.parseFloat(longitude)+0.00001, Float.parseFloat(latitude)+0.00001, 0);
                pathData.endPathData();

                NMapPathLineStyle pathLineStyle = new NMapPathLineStyle(mMapView.getContext());
                pathLineStyle.setLineColor(0xA04DD2, 0xff);
                pathLineStyle.setFillColor(0xFFFFFF,0x00);
                pathData.setPathLineStyle(pathLineStyle);

                NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay(pathData);

                // show all path data
                pathDataOverlay.showAllPathData(mMapController.getZoomLevel());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 원과 점 사이의 거리로 원 안의 포함여부 계산한다
     * meanshift로 원 위치 계속 옮기고 마지막 한 번만 그리는 알고리즘
     */
    protected void meanShift(double initLong, double initLati, float radius) {
        double dataDis;
        double sumLong = initLong;         //원 안에 속한 점이면 계속 더해줄 위도
        double sumLati = initLati;         //원 안에 속한 점이면 계속 더해줄 경도
        int count;                         //원 한 번 계산해줄때마다 sumLong과 sumLati 나눠줄 count
        NGeoPoint circleCenter;
        Point outPoint = null;

        try {
            /* 제이슨 받아오는 부분 matchData 함수랑 겹치는 곳 나중에 빼줄 것
               일단 혹시 몰라서 놔둠 */

            JSONObject jsonObj = new JSONObject(myJSON);
            location = jsonObj.getJSONArray(TAG_RESULTS);

            for(int k = 0; k < location.length(); k++) {
                count=1;
                for (int i = 0; i < location.length(); i++) {
                    JSONObject c = location.getJSONObject(i);
                    textNum = c.optString(TAG_TEXT_NUM);
                    longitude = c.optString(TAG_LONGITUDE);
                    latitude = c.optString(TAG_LATITUDE);

                    NGeoPoint point = new NGeoPoint(Double.parseDouble(longitude), Double.parseDouble(latitude));
                    outPoint = mMapView.getMapProjection().toPixels(point, outPoint);

                    if (outPoint.x <= 1650 && outPoint.x >= -550 && outPoint.y >= -900 && outPoint.y <= 2700) { //화면 안에 보이는 경우
                        circleCenter = new NGeoPoint((sumLong / count), (sumLati / count));
                        dataDis = NGeoPoint.getDistance(point, circleCenter); //원과 점 사이의 거리

                        if (dataDis < radius) { //원 안에 있으면
                            count++;
                            sumLong = sumLong + Double.parseDouble(longitude);
                            sumLati = sumLati + Double.parseDouble(latitude);
                        }
                    }
                }

                NMapPathDataOverlay pathDataOverlay = mOverlayManager.createPathDataOverlay();

                NMapCircleData circleData = new NMapCircleData(1);
                if(sumLong != initLong || sumLati != initLati) {
                    if( k == location.length() - 1) {
                        circleData.initCircleData();
                        //circleData.addCirclePoint(sumLong / count, sumLati / count, radius * (count/5)); //중심, 반지름 //원생성!!!
                        circleData.addCirclePoint(sumLong / count, sumLati / count, radius); //중심, 반지름 //원생성!!!
                        circleData.endCircleData();
                        pathDataOverlay.addCircleData(circleData);

                        NMapCircleStyle circleStyle = new NMapCircleStyle(mMapView.getContext());
                        circleStyle.setFillColor(0x000000, 0x00);
                        circleData.setCircleStyle(circleStyle);

                    }
                    circleData.setRendered(true);

                    //pathDataOverlay.showAllPathData(mMapController.getZoomLevel()); //줌이랑 센터 영향
                    sumLong = sumLong / count;
                    sumLati = sumLati / count;
                }
            }

            Log.i(LOG_TAG,"마지막 중심좌표! = " + sumLong + " , " + sumLati);

        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * 지도가 초기화된 후 호출된다.
     * 정상적으로 초기화되면 errorInfo 객체는 null이 전달되며,
     * 초기화 실패 시 errorInfo객체에 에러 원인이 전달된다
     */
    @Override
    public void onMapInitHandler(NMapView mapview, NMapError errorInfo) {

        if (errorInfo == null) { // success
            mMapController.setMapCenter(
                    new NGeoPoint(127.061, 37.51), 11);
            Log.i(LOG_TAG, "inithandler : zoomlevel = "+mapview.getMapController().getZoomLevel());
            isInit=true;
        } else { // fail
            android.util.Log.e("NMAP", "onMapInitHandler: error="
                    + errorInfo.toString());
        }
    }

    /**
     * 지도 레벨 변경 시 호출되며 변경된 지도 레벨이 파라미터로 전달된다.
     */
    @Override
    public void onZoomLevelChange(NMapView mapview, int level) {
        if(isInit){
            mapview.getOverlays().clear();
            /*
            meanShift(mapview.getMapController().getMapCenter().longitude,
                    mapview.getMapController().getMapCenter().latitude, 900f);
            */
            NGeoPoint LTPoint = mMapView.getMapProjection().fromPixels(0,0);
            NGeoPoint LMPoint = mMapView.getMapProjection().fromPixels(0,900);
            NGeoPoint LBPoint = mMapView.getMapProjection().fromPixels(0, 1800);

            NGeoPoint RTPoint = mMapView.getMapProjection().fromPixels(1100,0);
            NGeoPoint RMPoint = mMapView.getMapProjection().fromPixels(1100,900);
            NGeoPoint RBPoint = mMapView.getMapProjection().fromPixels(1100,1800);

            meanShift(LTPoint.longitude,LTPoint.latitude,1000.0F*(15-level));
            meanShift(LMPoint.longitude,LMPoint.latitude,1000f*(15-level));
            meanShift(LBPoint.longitude,LBPoint.latitude,1000f*(15-level));

            meanShift(RTPoint.longitude,RTPoint.latitude,1000f*(15-level));
            meanShift(RMPoint.longitude,RMPoint.latitude,1000f*(15-level));
            meanShift(RBPoint.longitude,RBPoint.latitude,1000f*(15-level));

            Log.i(LOG_TAG, "zoomLevel = "+level);
            Log.i(LOG_TAG, "Z: center-longitude : " + mapview.getMapController().getMapCenter().longitude);
            Log.i(LOG_TAG, "Z: center-latitude : " + mapview.getMapController().getMapCenter().latitude);
        }
    }

    /**
     * 지도 중심 변경 시 호출되며 변경된 중심 좌표가 파라미터로 전달된다.
     */
    @Override
    public void onMapCenterChange(NMapView mapview, NGeoPoint center) {
        int level = mMapController.getZoomLevel();

        if(isInit){
            mapview.getOverlays().clear();

            //meanShift(center.longitude, center.latitude, 900f);

            NGeoPoint LTPoint = mMapView.getMapProjection().fromPixels(0,0);
            NGeoPoint LMPoint = mMapView.getMapProjection().fromPixels(0,900);
            NGeoPoint LBPoint = mMapView.getMapProjection().fromPixels(0, 1800);

            NGeoPoint RTPoint = mMapView.getMapProjection().fromPixels(1100,0);
            NGeoPoint RMPoint = mMapView.getMapProjection().fromPixels(1100,900);
            NGeoPoint RBPoint = mMapView.getMapProjection().fromPixels(1100,1800);

            meanShift(LTPoint.longitude,LTPoint.latitude,1000f*(15-level));
            meanShift(LMPoint.longitude,LMPoint.latitude,1000f*(15-level));
            meanShift(LBPoint.longitude,LBPoint.latitude,1000f*(15-level));

            meanShift(RTPoint.longitude,RTPoint.latitude,1000f*(15-level));
            meanShift(RMPoint.longitude,RMPoint.latitude,1000f*(15-level));
            meanShift(RBPoint.longitude,RBPoint.latitude,1000f*(15-level));

            Log.i(LOG_TAG, "C: center-longitude : " + String.valueOf(center.longitude));
            Log.i(LOG_TAG, "C: center-latitude : " + String.valueOf(center.latitude));
        }
    }

    /**
     * 지도 애니메이션 상태 변경 시 호출된다.
     * animType : ANIMATION_TYPE_PAN or ANIMATION_TYPE_ZOOM
     * animState : ANIMATION_STATE_STARTED or ANIMATION_STATE_FINISHED
     */
    @Override
    public void onAnimationStateChange(
            NMapView arg0, int animType, int animState) {
    }

    @Override
    public void onMapCenterChangeFine(NMapView arg0) {
    }

    public NMapCalloutOverlay onCreateCalloutOverlay(NMapOverlay itemOverlay, NMapOverlayItem overlayItem, Rect itemBounds) {
        // set your callout overlay
        return new NMapCalloutBasicOverlay(itemOverlay, overlayItem, itemBounds);
    }

    /* POI data State Change Listener*/
    private final NMapPOIdataOverlay.OnStateChangeListener onPOIdataStateChangeListener = new NMapPOIdataOverlay.OnStateChangeListener() {

        public void onCalloutClick(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            if (DEBUG) {
                Log.i(LOG_TAG, "onCalloutClick: title=" + item.getTitle());
            }

            // [[TEMP]] handle a click event of the callout
            //Toast.makeText(MainActivity.this, "onCalloutClick: " + item.getTitle(), Toast.LENGTH_LONG).show();
            Intent intent;
            intent = new Intent(MeanShiftActivity.this, TextActivity.class);
            startActivity(intent);
        }

        public void onFocusChanged(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            if (DEBUG) {
                if (item != null) {
                    Log.i(LOG_TAG, "onFocusChanged: " + item.toString());
                } else {
                    Log.i(LOG_TAG, "onFocusChanged: ");
                }
            }
        }
    };

    private final NMapOverlayManager.OnCalloutOverlayListener onCalloutOverlayListener = new NMapOverlayManager.OnCalloutOverlayListener() {

        public NMapCalloutOverlay onCreateCalloutOverlay(NMapOverlay itemOverlay, NMapOverlayItem overlayItem,
                                                         Rect itemBounds) {

            // handle overlapped items
            if (itemOverlay instanceof NMapPOIdataOverlay) {
                NMapPOIdataOverlay poiDataOverlay = (NMapPOIdataOverlay) itemOverlay;

                // check if it is selected by touch event
                if (!poiDataOverlay.isFocusedBySelectItem()) {
                    int countOfOverlappedItems = 1;

                    NMapPOIdata poiData = poiDataOverlay.getPOIdata();
                    for (int i = 0; i < poiData.count(); i++) {
                        NMapPOIitem poiItem = poiData.getPOIitem(i);

                        // skip selected item
                        if (poiItem == overlayItem) {
                            continue;
                        }

                        // check if overlapped or not
                        if (Rect.intersects(poiItem.getBoundsInScreen(), overlayItem.getBoundsInScreen())) {
                            countOfOverlappedItems++;
                        }
                    }

                    if (countOfOverlappedItems > 1) {
                        String text = countOfOverlappedItems + " overlapped items for " + overlayItem.getTitle();
                        Toast.makeText(MeanShiftActivity.this, text, Toast.LENGTH_LONG).show();
                        return null;
                    }
                }
            }

            // use custom old callout overlay
            if (overlayItem instanceof NMapPOIitem) {
                NMapPOIitem poiItem = (NMapPOIitem) overlayItem;

                /*if (poiItem.showRightButton()) {
                    return new NMapCalloutCustomOldOverlay(itemOverlay, overlayItem, itemBounds,
                           mMapViewerResourceProvider);
                }*/
            }

            // use custom callout overlay
            return new NMapCalloutCustomOverlay(itemOverlay, overlayItem, itemBounds, mMapViewerResourceProvider);

            // set basic callout overlay
            // return new NMapCalloutBasicOverlay(itemOverlay, overlayItem, itemBounds);
        }
    };
    /* NMapDataProvider Listener */
    private final OnDataProviderListener onDataProviderListener = new OnDataProviderListener() {

        public void onReverseGeocoderResponse(NMapPlacemark placeMark, NMapError errInfo) {

            //if (DEBUG) {
            Log.i(LOG_TAG, "onReverseGeocoderResponse: placeMark="
                    + ((placeMark != null) ? placeMark.toString() : null));
            //}

            if (errInfo != null) {
                Log.e(LOG_TAG, "Failed to findPlacemarkAtLocation: error=" + errInfo.toString());

                Toast.makeText(MeanShiftActivity.this, errInfo.toString(), Toast.LENGTH_LONG).show();
                return;
            }

            if (mFloatingPOIitem != null && mFloatingPOIdataOverlay != null) {
                mFloatingPOIdataOverlay.deselectFocusedPOIitem();

                if (placeMark != null) {
                    mFloatingPOIitem.setTitle(placeMark.toString());
                }
                mFloatingPOIdataOverlay.selectPOIitemBy(mFloatingPOIitem.getId(), false);
            }
        }
    };

    private final NMapPOIdataOverlay.OnFloatingItemChangeListener onPOIdataFloatingItemChangeListener = new NMapPOIdataOverlay.OnFloatingItemChangeListener() {

        @Override
        public void onPointChanged(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
            NGeoPoint point = item.getPoint();
            Point outPoint = null;
            //if (DEBUG) {
            Log.i(LOG_TAG, "onPointChanged: point=" + point.toString());
            //}

            outPoint=mMapView.getMapProjection().toPixels(point,outPoint);
            Log.i(LOG_TAG, "outPoint="+outPoint.toString());

            findPlacemarkAtLocation(point.longitude, point.latitude);

            item.setTitle(null);
        }
    };
    public void getData(String url) {
        class getDataJSON extends AsyncTask<String, Integer, String> {
            @Override
            protected String doInBackground(String... urls) {
                String uri = urls[0];
                BufferedReader bufferedReader = null;
                try {
                    URL url = new URL(uri);

                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    StringBuilder sb = new StringBuilder();

                    bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String json;
                    while((json = bufferedReader.readLine())!=null){
                        sb.append(json+"\n");
                    }
                    return sb.toString().trim();
                } catch(Exception e) {
                    return null;
                }
            }
            protected  void onPostExecute(String result) {
                NGeoPoint LTPoint = mMapView.getMapProjection().fromPixels(0,0);
                NGeoPoint LMPoint = mMapView.getMapProjection().fromPixels(0,900);
                NGeoPoint LBPoint = mMapView.getMapProjection().fromPixels(0, 1800);

                NGeoPoint RTPoint = mMapView.getMapProjection().fromPixels(1100,0);
                NGeoPoint RMPoint = mMapView.getMapProjection().fromPixels(1100,900);
                NGeoPoint RBPoint = mMapView.getMapProjection().fromPixels(1100,1800);

                myJSON = result;

                long startTime = System.currentTimeMillis();

                matchData();
                meanShift(LTPoint.longitude, LTPoint.latitude, 1000f);

                long endTime = System.currentTimeMillis();
                long Total = endTime - startTime;
                Log.i(LOG_TAG, "Time : "+Total+" (ms) ");

                meanShift(LMPoint.longitude,LMPoint.latitude,1000f);
                meanShift(LBPoint.longitude,LBPoint.latitude,1000f);

                meanShift(RTPoint.longitude,RTPoint.latitude,1000f);
                meanShift(RMPoint.longitude,RMPoint.latitude,1000f);
                meanShift(RBPoint.longitude,RBPoint.latitude,1000f);
            }
        }
        getDataJSON g = new getDataJSON();
        g.execute(url);

    }
}