# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\qi\Documents\软件\android\android-sdk-windows/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keep class android.support.v7.widget.SearchView { *; }
-keep class android.support.v7.widget.LinearLayoutManager { *; }
-keep class !android.support.v7.internal.view.menu.**,android.support.v7.** {*;}
#-keep class !android.support.design.internal.NavigationMenu,!android.support.design.internal.NavigationMenuPresenter,!android.support.design.internal.NavigationSubMenu,** {*;}
-keep class android.content.pm.IPackageStatsObserver { *; }

#picasso
-dontwarn com.squareup.okhttp.**

#混淆字典
-obfuscationdictionary dic.txt
-classobfuscationdictionary dic.txt
-packageobfuscationdictionary dic.txt
-repackageclasses

#okhttp
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*
# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform

#mars-log
-keep class com.tencent.mars.xlog.** { *; }

#firebase crash
#https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports?authuser=0#android
#-keepattributes *Annotation*
#-keepattributes SourceFile,LineNumberTable
#-keep public class * extends java.lang.Exception

-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}



# library中的proguard无效：https://stackoverflow.com/a/10992604/1263423


# inmobi
##-keepattributes SourceFile,LineNumberTable
#-keep class com.inmobi.** { *; }
#-dontwarn com.inmobi.**
#-keep public class com.google.android.gms.**
#-dontwarn com.google.android.gms.**
#-dontwarn com.squareup.picasso.**
#-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient{
#     public *;
#}
#-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info{
#     public *;
#}
## skip the Picasso library classes
#-keep class com.squareup.picasso.** {*;}
#-dontwarn com.squareup.picasso.**
#-dontwarn com.squareup.okhttp.**
## skip Moat classes
#-keep class com.moat.** {*;}
#-dontwarn com.moat.**
## skip AVID classes
#-keep class com.integralads.avid.library.* {*;}



# tencent adnet (https://developers.adnet.qq.com/doc/android/access_doc)
#-keep class com.qq.e.** {
#    public protected *;
#}
#-keep class android.support.v4.**{
#    public *;
#}


#-keep class android.support.v7.**{
#    public *;
#}
#-keep class MTT.ThirdAppInfoNew {
#    *;
#}
#-keep class com.tencent.** {
#    *;
#}


-keep class * implements cn.wq.myandroidtoolspro.helper.AdHelperInterface


-dontwarn moe.shizuku.**




#glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
