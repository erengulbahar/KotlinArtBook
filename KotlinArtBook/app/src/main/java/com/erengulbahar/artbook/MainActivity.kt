package com.erengulbahar.artbook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.erengulbahar.artbook.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity()
{
    private lateinit var binding : ActivityMainBinding
    private lateinit var artList : ArrayList<ArtModel>
    private lateinit var artAdapter : ArtAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        artList = ArrayList()

        artAdapter = ArtAdapter(artList)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = artAdapter

        try
        {
            val myDatabase = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)
            val cursor = myDatabase.rawQuery("SELECT * FROM arts",null)

            var artNameIndex = cursor.getColumnIndex("artname")
            var idIndex = cursor.getColumnIndex("id")

            while(cursor.moveToNext())
            {
                var name = cursor.getString(artNameIndex)
                var id = cursor.getInt(idIndex)
                var art = ArtModel(name,id)

                artList.add(art)
            }

            artAdapter.notifyDataSetChanged()

            cursor.close()
        }

        catch (e : Exception)
        {
            println(e.toString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuInflater.inflate(R.menu.art_menu,menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        if(item.itemId == R.id.addArt)
        {
            val intent = Intent(this,ArtActivity::class.java)
            intent.putExtra("info","new")
            startActivity(intent)
        }

        return super.onOptionsItemSelected(item)
    }
}