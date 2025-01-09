package com.example.procrastimates;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class DotDrawable extends Drawable {

    private final Paint paint;

    public DotDrawable(int color) {
        paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(Canvas canvas) {
        // Desenează un punct în mijlocul zonei disponibile
        float radius = 10f; // Ajustează dimensiunea punctului
        canvas.drawCircle(getBounds().exactCenterX(), getBounds().exactCenterY(), radius, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.OPAQUE;
    }
}

