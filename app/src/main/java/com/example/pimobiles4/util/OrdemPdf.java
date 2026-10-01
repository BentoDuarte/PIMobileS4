package com.example.pimobiles4.util;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import com.example.pimobiles4.model.OrdemServico;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
/** Exportação com quebra de linhas e paginação; usa APIs nativas do Android. */
public final class OrdemPdf {
    private OrdemPdf(){}
    public static void escrever(OrdemServico o,OutputStream out) throws IOException {
        String data=new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date(o.getCriadaEm()));
        String texto="Ordem de Serviço #"+o.getId()+"\n"+o.getTitulo()+"\n\nStatus: "+o.getStatus()+"\nPrioridade: "+o.getPrioridade()+"\nCriada em: "+data+"\n\nCliente: "+o.getCliente().getNome()+"\nTelefone: "+o.getCliente().getTelefone()+"\nE-mail: "+o.getCliente().getEmail()+"\nResponsável: "+o.getResponsavel()+"\n\nDescrição\n"+o.getDescricao();
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setTextSize(12);paint.setColor(Color.rgb(23,37,61));
        try {
            PdfDocument pdf = new PdfDocument();
            PdfDocument.Page page=null;int y=800,n=0;
            for(String paragrafo:texto.split("\n",-1)){
                String resto=paragrafo;
                do {
                    if(y>780){
                        if(page!=null)pdf.finishPage(page);
                        page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,++n).create()); y=48;
                        page.getCanvas().drawText("SERVIÇO MOBILE • OS #"+o.getId()+" • Página "+n,40,815,paint);
                    }
                    int count=resto.isEmpty()?0:paint.breakText(resto,true,515,null);
                    if(count==0&&!resto.isEmpty())count=1;
                    if(count<resto.length()){int space=resto.lastIndexOf(' ',count);if(space>0)count=space;}
                    page.getCanvas().drawText(resto.substring(0,count),40,y,paint);y+=19;
                    resto=resto.substring(count).trim();
                }while(!resto.isEmpty());
            }
            if (page != null) {
                pdf.finishPage(page);
            }

            pdf.writeTo(out);
        } finally {
            pdf.close();
        }
    }
}
