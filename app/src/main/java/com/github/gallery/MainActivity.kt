package com.github.gallery

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val str0 = "http://pic40.nipic.com/20140331/16753510_161247285197_2.jpg"
    val str1 = "http://www.m1ok.com/upload/netpic2/2017-09-06-11-25-43643416497974.jpg"
    val str2 = "http://pic60.nipic.com/file/20150209/5533146_170627166000_2.jpg"
    val str3 = "http://pic44.nipic.com/20140719/12728082_175822220000_2.jpg"
    val str4 = "http://pic1.win4000.com/pic/4/22/fc5d1297297.jpg"
    val str5 = "http://pic18.nipic.com/20111228/5817249_153010533155_2.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val array = arrayOf(
            Uri.parse(str0),
            Uri.parse(str1),
            Uri.parse(str2),
            Uri.parse(str3),
            Uri.parse(str4),
            Uri.parse(str5)
        )

        btn.setOnClickListener { Gallery.showGallery(supportFragmentManager, 0, array) }
    }
}
