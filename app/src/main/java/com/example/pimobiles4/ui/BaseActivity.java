package com.example.pimobiles4.ui;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import com.example.pimobiles4.MainActivity;
import com.example.pimobiles4.R;
public abstract class BaseActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if(!getSharedPreferences("sessao",MODE_PRIVATE).getBoolean("demo",false)) {
            startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish();
        }
    }
    protected void tela(int layout) {
        EdgeToEdge.enable(this); setContentView(layout);
        android.view.View root=findViewById(R.id.main);
        final int left=root.getPaddingLeft(),top=root.getPaddingTop(),right=root.getPaddingRight(),bottom=root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,w)->{
            Insets s=w.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.ime());
            v.setPadding(left+s.left,top+s.top,right+s.right,bottom+s.bottom); return w;
        });
    }
    protected void aviso(Exception e) { Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show(); }
}
