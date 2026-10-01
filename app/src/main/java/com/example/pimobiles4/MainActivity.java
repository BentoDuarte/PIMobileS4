package com.example.pimobiles4;
import android.os.Bundle;
import android.content.Intent;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import com.example.pimobiles4.ui.ListaOrdensActivity;
/** Login demonstrativo, sem autenticação de servidor. */
public class MainActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if(getSharedPreferences("sessao",MODE_PRIVATE).getBoolean("demo",false)) { abrir(); return; }
        EdgeToEdge.enable(this); setContentView(R.layout.activity_main);
        android.view.View root=findViewById(R.id.main);
        final int left=root.getPaddingLeft(),top=root.getPaddingTop(),right=root.getPaddingRight(),bottom=root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,w)->{
            Insets s=w.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.ime());
            v.setPadding(left+s.left,top+s.top,right+s.right,bottom+s.bottom);return w;
        });
        findViewById(R.id.entrar).setOnClickListener(v->{
            EditText email=findViewById(R.id.email), senha=findViewById(R.id.senha);
            if(!email.getText().toString().trim().equalsIgnoreCase("demo@os.com")) {email.setError("Use demo@os.com");return;}
            if(!senha.getText().toString().equals("demo123")) {senha.setError("Use demo123");return;}
            getSharedPreferences("sessao",MODE_PRIVATE).edit().putBoolean("demo",true).apply(); abrir();
        });
    }
    private void abrir() {startActivity(new Intent(this,ListaOrdensActivity.class));finish();}
}
