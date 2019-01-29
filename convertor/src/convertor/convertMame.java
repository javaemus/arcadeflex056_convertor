/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package convertor;

import static convertor.Convertor.inpos;
import static convertor.Convertor.token;
import static convertor.sUtil.getToken;
import static convertor.sUtil.parseChar;
import static convertor.sUtil.parseToken;
import static convertor.sUtil.skipLine;
import static convertor.sUtil.skipSpace;

/**
 * @author shadow
 */
public class convertMame {

    public static void ConvertMame() {
        Analyse();
        Convert();
    }

    public static void Analyse() {

    }
    static final int PLOT_BOX = 1;
    static final int PLOT_PIXEL = 2;
    static final int MARK_DIRTY = 3;

    public static void Convert() {
        Convertor.inpos = 0;//position of pointer inside the buffers
        Convertor.outpos = 0;
        boolean only_once_flag = false;//gia na baleis to header mono mia fora
        boolean line_change_flag = false;

        int kapa = 0;
        int i = 0;
        int type = 0;
        int i3 = -1;
        int i8 = -1;
        int type2 = 0;
        int[] insideagk = new int[10];//get the { that are inside functions

        do {
            if (Convertor.inpos >= Convertor.inbuf.length)//an to megethos einai megalitero spase to loop
            {
                break;
            }
            char c = sUtil.getChar(); //pare ton character
            if (line_change_flag) {
                for (int i1 = 0; i1 < kapa; i1++) {
                    sUtil.putString("\t");
                }

                line_change_flag = false;
            }
            switch (c) {
                case 35: // '#'
                {
                    if (!sUtil.getToken("#include"))//an den einai #include min to trexeis
                    {
                        break;
                    }
                    sUtil.skipLine();
                    if (!only_once_flag)//trekse auto to komati mono otan bris to proto include
                    {
                        only_once_flag = true;
                        sUtil.putString("/*\r\n");
                        sUtil.putString(" * ported to v" + Convertor.mameversion + "\r\n");
                        sUtil.putString(" * using automatic conversion tool v" + Convertor.convertorversion + "\r\n");
                        /*sUtil.putString(" * converted at : " + Convertor.timenow() + "\r\n");*/
                        sUtil.putString(" */ \r\n");
                        sUtil.putString("package " + Convertor.packageName + ";\r\n");
                        sUtil.putString("\r\n");
                        sUtil.putString((new StringBuilder()).append("public class ").append(Convertor.className).append("\r\n").toString());
                        sUtil.putString("{\r\n");
                        kapa = 1;
                        line_change_flag = true;
                    }
                    continue;
                }
                case 10: // '\n'
                {
                    Convertor.outbuf[Convertor.outpos++] = Convertor.inbuf[Convertor.inpos++];
                    line_change_flag = true;
                    continue;
                }
                case 'i': {
                    int l1 = inpos;
                    if (getToken("if")) {
                        skipSpace();
                        if (parseChar() != '(') {
                            inpos = l1;
                            break;
                        }
                        skipSpace();
                        char c2 = parseChar();
                        if (c2 == '!') {
                            skipSpace();
                            token[0] = parseToken();
                            skipSpace();
                            if (parseChar() != ')') {
                                inpos = l1;
                                break;
                            }
                            sUtil.putString((new StringBuilder()).append("if (").append(token[0]).append(" == 0)").toString());
                        } else {
                            inpos = l1;
                            break;
                        }
                    } else {
                        break;
                    }
                    continue;
                }
                case 'e': {
                    int j1 = inpos;
                    if (getToken("enum")) {
                        skipSpace();

                        if (parseChar() != '{') {
                            inpos = j1;
                            break;
                        }
                        skipSpace();
                        int i5 = 0;
                        char c2;
                        do {
                            token[i5++] = parseToken();
                            skipSpace();
                            c2 = parseChar();
                            if (c2 != '}' && c2 != ',') {
                                inpos = j1;
                                break;
                            }
                            skipSpace();
                        } while (c2 == ',');
                        if (parseChar() != ';') {
                            inpos = j1;
                            break;
                        }
                        int k5 = 0;
                        while (k5 < i5) {
                            sUtil.putString((new StringBuilder()).append("public static final int ").append(token[k5]).append(" = ").append(k5).append(";\n\t").toString());
                            k5++;
                        }
                    } else {
                        break;
                    }
                    continue;
                }
                case 's': {
                    i = Convertor.inpos;
                    if (sUtil.getToken("static")) {
                        sUtil.skipSpace();
                    }
                    if (!sUtil.getToken("struct")) //static but not static struct
                    {
                        if (sUtil.getToken("void")) {
                            sUtil.skipSpace();
                            Convertor.token[0] = sUtil.parseToken();
                            sUtil.skipSpace();
                            if (sUtil.parseChar() != '(') {
                                Convertor.inpos = i;
                                break;
                            }
                            sUtil.skipSpace();
                            if (sUtil.getToken("struct mame_bitmap *b,int x,int y,int w,int h,UINT32 p")) {
                                sUtil.skipSpace();
                                if (sUtil.parseChar() != ')') {
                                    Convertor.inpos = i;
                                    break;
                                }
                                if (sUtil.getChar() == ';') {
                                    sUtil.skipLine();
                                    continue;
                                }
                                if (Convertor.token[0].contains("pb_")) {
                                    sUtil.putString((new StringBuilder()).append("public static plot_box_procPtr ").append(Convertor.token[0]).append("  = new plot_box_procPtr() { public void handler(mame_bitmap b, int x, int y, int w, int h, /*UINT32*/int p) ").toString());
                                    type = PLOT_BOX;
                                    i3 = -1;
                                    continue;
                                }

                            }
                            if (sUtil.getToken("struct mame_bitmap *b,int x,int y,UINT32 p")) {
                                sUtil.skipSpace();
                                if (sUtil.parseChar() != ')') {
                                    Convertor.inpos = i;
                                    break;
                                }
                                if (sUtil.getChar() == ';') {
                                    sUtil.skipLine();
                                    continue;
                                }
                                if (Convertor.token[0].contains("pp_")) {
                                    sUtil.putString((new StringBuilder()).append("public static plot_pixel_procPtr ").append(Convertor.token[0]).append("  = new plot_pixel_procPtr() { public void handler(mame_bitmap b,int x,int y,/*UINT32*/int p) ").toString());
                                    type = PLOT_PIXEL;
                                    i3 = -1;
                                    continue;
                                }

                            }
                            if (sUtil.getToken("int sx,int sy,int ex,int ey")) {
                                sUtil.skipSpace();
                                if (sUtil.parseChar() != ')') {
                                    Convertor.inpos = i;
                                    break;
                                }
                                if (sUtil.getChar() == ';') {
                                    sUtil.skipLine();
                                    continue;
                                }
                                if (Convertor.token[0].contains("md")) {
                                    sUtil.putString((new StringBuilder()).append("public static mark_dirty_procPtr ").append(Convertor.token[0]).append("  = new mark_dirty_procPtr() { public void handler(int sx,int sy,int ex,int ey) ").toString());
                                    type = MARK_DIRTY;
                                    i3 = -1;
                                    continue;
                                }

                            }
                        }
                        Convertor.inpos = i;
                        break;
                    } else {
                        Convertor.inpos = i;
                        break;
                    }
                }
                case '{': {
                    if (type == PLOT_PIXEL || type == MARK_DIRTY
                            || type == PLOT_BOX) {
                        i3++;
                    }
                }
                break;
                case '}': {
                    if (type == PLOT_PIXEL || type == MARK_DIRTY || type == PLOT_BOX) {
                        i3--;
                        if (i3 == -1) {
                            sUtil.putString("} };");
                            Convertor.inpos += 1;
                            type = -1;
                            continue;
                        }
                    }
                }
            }
            Convertor.outbuf[Convertor.outpos++] = Convertor.inbuf[Convertor.inpos++];//grapse to inputbuffer sto output
        } while (true);
        if (only_once_flag) {
            sUtil.putString("}\r\n");
        }
    }
}
