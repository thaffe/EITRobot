#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <vector>
#include <android/log.h>

#define LOG_TAG "ROBOT"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
#define PI 3.14159265359

using namespace std;
using namespace cv;

int structSize = 9;
Scalar minColors [] = {Scalar(0,90,100) , Scalar(110, 120, 10)};
Scalar maxColors [] = {Scalar(10,255,255), Scalar(130, 255, 200)};


inline Scalar getMax(int color)
{
  return maxColors[color];
}

inline Scalar getMin(int color)
{
  return minColors[color];
}


extern "C" {
   jobject get_features(JNIEnv* env, jobject obj, jlong addrRgba, jint color, jint type)
{
      Mat& mRgb = *(Mat*)addrRgba;
     Mat hsv;

     cvtColor(mRgb,hsv,CV_RGB2HSV);
     Mat thresh;
     inRange(hsv,getMin(color), getMax(color), thresh);

     Mat element = getStructuringElement(MORPH_RECT,
        Size( structSize , structSize),
        Point( 0,0) );

     erode(thresh, thresh, element );
     dilate(thresh, thresh, element );
     blur( thresh, thresh, Size(3,3) );
    // cvtColor(thresh, mRgb, CV_GRAY2RGB);

     vector<vector<Point> > contours;
     vector<Vec4i> hierarchy;
     
     findContours(thresh, contours, hierarchy, CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE, Point(0, 0) );


      int bestIndex = -1; 
      int bestSize = -1;

      LOGD("C++ Process started");
      if(!contours.size()) return NULL;

      jclass clsArrayList = env->FindClass("java/util/ArrayList");
      jmethodID constructor = env->GetMethodID(clsArrayList, "<init>", "()V");
      jobject objArrayList = env->NewObject(clsArrayList, constructor, "");
      jclass cls;
      jmethodID arrayListAdd = env->GetMethodID(clsArrayList, "add", "(Ljava/lang/Object;)Z");
      
      if(type == 1){
         cls = env->FindClass("com/eit/image/CollectionBox");
         constructor = env->GetMethodID(cls, "<init>", "(IIII)V");
      }else{
         cls = env->FindClass("com/eit/image/Ball");
         constructor = env->GetMethodID(cls, "<init>", "(IIII)V");
      }

      // vector<Point>shape(50);
      // for(int i = 0; i < 50; i++){
      //    double angle = 4*3.1415926535897932384626433832795/50*i;
      //    shape[i] = Point(cos(angle),sin(angle));
      // }
      // hasShape = true;

       /// Approximate contours to polygons + get bounding rects and circles
     vector<vector<Point> > contours_poly( contours.size() );
     vector<Rect> boundRect( contours.size() );
     vector<Point2f>center( contours.size() );
     vector<float>radius( contours.size() );
     vector<int>shapeType(contours.size());
     //vector<double>match(contours.size());
     for( int i = 0; i < contours.size(); i++ )
      { 

         approxPolyDP( Mat(contours[i]), contours_poly[i], 3, true );
         // if(test && i == 0){
         //    shape = contours_poly[i];
         //    hasShape = true;
         //    LOGD("ROBOT find shape");
         // }

         // match[i] = matchShapes(contours_poly[i],shape,CV_CONTOURS_MATCH_I1,0.0);

         boundRect[i] = boundingRect( Mat(contours_poly[i]) );
         minEnclosingCircle( (Mat)contours_poly[i], center[i], radius[i] );

         double circleArea = (2*radius[i]) * (2*radius[i]);
         double rectArea = boundRect[i].area();

         if(circleArea > rectArea){
            if(contours_poly[i].size() < 8)
               shapeType[i] = 1;
            else
               shapeType[i] = 0;
         }else{
            shapeType[i] = 0;
         }

         LOGD(isContourConvex(contours_poly[i]) ? "Is Convex" : "NOPE");
         if(shapeType[i] == type && isContourConvex(contours_poly[i])){
            jobject obj;
            if(type == 1)
               obj = env->NewObject(cls, constructor,(int)center[i].x, (int)center[i].y, (int)boundRect[i].width, (int)boundRect[i].height,(int)type);
            else
               obj = env->NewObject(cls, constructor,(int)center[i].x, (int)center[i].y, (int)radius[i],(int)type);

            env->CallObjectMethod(objArrayList, arrayListAdd, obj);
         }


         double size = shapeType[i] ? boundRect[i].size().height : radius[i];
         if(bestSize < size){
            bestIndex = i;
            bestSize = size;
         }

      }

      int fontFace = FONT_HERSHEY_PLAIN;
      double fontScale = 1.5;
      int thickness = 2;
      string text;

     

       /// Draw polygonal contour + bonding rects + circles
      for( int i = 0; i< contours.size(); i++ )
      {
         Scalar color = Scalar(0, 255,0,0);
         rectangle(mRgb, boundRect[i].tl(), boundRect[i].br(), color, 1, 8, 0 );
         circle(mRgb, center[i], (int)radius[i], color, 1, 8, 0 );

         if(shapeType[i]) text = "Rect";
         else text ="Circle";
         
         // then put the text itself
         putText(mRgb, text, center[i], fontFace, fontScale,
                 Scalar::all(255), thickness, 8);

         drawContours( mRgb, contours_poly, i, color, 1, 8, vector<Vec4i>(), 0, Point() );
         
      }

      

      return objArrayList;
   }


   JNIEXPORT jobject Java_com_eit_image_ImageProcessing_FindFeatures(JNIEnv* env, jobject obj, jlong addrRgba, jint color, jint type, jint test);

   JNIEXPORT jobject JNICALL Java_com_eit_image_ImageProcessing_FindFeatures(JNIEnv* env, jobject obj, jlong addrRgba, jint color, jint type, jint test)
   {
      return get_features(env,obj, addrRgba, color, type);
   }

   JNIEXPORT jobject Java_com_eit_image_ImageProcess_FindFeatures(JNIEnv* env, jobject obj, jlong addrRgba, jint color, jint type);

   JNIEXPORT jobject JNICALL Java_com_eit_image_ImageProcess_FindFeatures(JNIEnv* env, jobject obj, jlong addrRgba, jint color, jint type)
   {
      
      return get_features(env,obj, addrRgba, color, type);
   }
}
