package com.erengulbahar.artbook

import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.erengulbahar.artbook.databinding.ActivityArtBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.util.jar.Manifest

class ArtActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityArtBinding
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    var selectedBitMap : Bitmap? = null
    private lateinit var myDatabase : SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        myDatabase = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)

        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")

        if(info.equals("new"))
        {
            binding.artNameText.setText("")
            binding.artistNameText.setText("")
            binding.yearText.setText("")
            binding.saveButton.visibility = View.VISIBLE
            binding.imageView.setImageResource(R.drawable.saveimage)
        }

        else
        {
            val selectedId = intent.getIntExtra("id",1)
            val cursor = myDatabase.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))
            var artNameIndex = cursor.getColumnIndex("artname")
            var artistNameIndex = cursor.getColumnIndex("artistname")
            var yearIndex = cursor.getColumnIndex("year")
            var imageIndex = cursor.getColumnIndex("image")

            while(cursor.moveToNext())
            {
                binding.artNameText.setText(cursor.getString(artNameIndex))
                binding.artistNameText.setText(cursor.getString(artistNameIndex))
                binding.yearText.setText(cursor.getString(yearIndex))

                var byteArray = cursor.getBlob(imageIndex)
                var bitMap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)

                binding.imageView.setImageBitmap(bitMap)
                binding.imageView.setOnClickListener {

                }
            }

            cursor.close()

            binding.saveButton.visibility = View.INVISIBLE
        }
    }

    fun saveData(view : View)
    {
        var artName = binding.artNameText.text.toString()
        var artistName = binding.artistNameText.text.toString()
        var year = binding.yearText.text.toString()

        if(selectedBitMap != null)
        {
            val smallBitmap = makeSmallerBitMap(selectedBitMap!!,300)

            //Görseli veriye çeviriyoruz burda
            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            try
            {
                //val myDatabase = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)

                myDatabase.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")

                val statement = myDatabase.compileStatement("INSERT INTO arts (artname, artistname, year, image) VALUES (?, ?, ?, ?)")
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()
            }

            catch (e : Exception)
            {
                println(e.toString())
            }

            val intent = Intent(this,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) //Ne kadar açık aktivite varsa kapatıyor
            startActivity(intent)
        }
    }

    private fun makeSmallerBitMap(image : Bitmap, maximumSize : Int) : Bitmap
    {
        var width = image.width
        var height = image.height
        val bitmapRatio : Double = width.toDouble() / height.toDouble()

        if(bitmapRatio > 1)
        {
            //Landscape
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()
        }

        else
        {
            //Portrait
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()
        }

        return Bitmap.createScaledBitmap(image,width,height,true)
    }

    fun selectImage(view : View)
    {
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.READ_EXTERNAL_STORAGE))
            {
                //İkinci kez izin isteme
                Snackbar.make(view,"Permission needed for gallery!",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",View.OnClickListener {
                    //İzin isteme
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }).show()
            }

            else
            {
                //İlk kez izin isteme
                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        else
        {
            val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        }
    }

    private fun registerLauncher()
    {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback {
                if(it.resultCode == RESULT_OK)
                {
                    val intentFromResult = it.data

                    if(intentFromResult != null)
                    {
                        val imageData = intentFromResult.data

                        if(imageData != null)
                        {
                            try
                            {
                                if(Build.VERSION.SDK_INT >= 28)
                                {
                                    val source = ImageDecoder.createSource(contentResolver,imageData)
                                    selectedBitMap = ImageDecoder.decodeBitmap(source)

                                    binding.imageView.setImageBitmap(selectedBitMap)
                                }

                                else
                                {
                                    selectedBitMap = MediaStore.Images.Media.getBitmap(contentResolver,imageData)

                                    binding.imageView.setImageBitmap(selectedBitMap)
                                }
                            }

                            catch (e : Exception)
                            {
                                println(e.toString())
                            }
                        }
                    }
                }
            })

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission(),
            ActivityResultCallback {
                if(it) //İzin verildiyse
                {
                    val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intentToGallery)
                }

                else
                {
                    Toast.makeText(this,"Permission needed!",Toast.LENGTH_LONG).show()
                }
            })
    }
}