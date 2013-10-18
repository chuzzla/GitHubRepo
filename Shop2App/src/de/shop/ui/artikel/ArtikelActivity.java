package de.shop.ui.artikel;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import de.shop.R;

@SuppressLint("Registered")
public class ArtikelActivity extends Activity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.artikel);
    }
}
