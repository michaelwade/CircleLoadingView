package com.michaelwade.circleloadingview

import android.animation.ValueAnimator
import android.animation.ValueAnimator.INFINITE
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
//import androidx.databinding.BindingAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

/**
 * author : wangbiao
 * e-mail : wangbiao@pycredit.cn
 * date   : 2020/11/7 16:33
 * desc   : 自定义转圈 loading
 */
class CircleLoadingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var valueAnimator: ValueAnimator? = null
    private var viewCenterX = 0            // view宽的中心点(可以暂时理解为圆心)
    private var viewCenterY = 0            // view高的中心点(可以暂时理解为圆心)
    private var innerCircleColor = 0       // 最里面圆的颜色
    private var ringWidth = 0f             // 圆环宽度
    private var ringBgColor = 0            // 圆环背景色
    private var progressSweepAngle = 0f    // 进度条弧形扫过的角度
    private var progressRingColor = 0      // 进度圆环颜色
    private var progressPercent = 0f       // 进度百分比f
    private var progressStartDegree = 0f   // 进度条起始位置的角度
    private var logoMarginBorder = 0f      // 中间logo边框离View边框的距离
    private var logoResId: Int = 0         // 中间 logo 资源id
    private var logoBmp: Bitmap? = null    // 中间 logo Bitmap
    private var paint: Paint               // 画笔
    private var rectF: RectF               // 画圆环需要的矩形
    private var autoRotate = true          // 是否自动旋转
    private var isAnimating = false          // 是否动画播放中
//    private var initAnimating = false          // 初始设置 是否动画播放中

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CircleLoadingView, 0, 0)
        ringWidth = a.getDimension(R.styleable.CircleLoadingView_ringWidth, 0f)
        innerCircleColor =
            a.getColor(R.styleable.CircleLoadingView_innerCircleColor, Color.TRANSPARENT)
        ringBgColor = a.getColor(R.styleable.CircleLoadingView_ringBgColor, Color.LTGRAY)
        progressRingColor = a.getColor(R.styleable.CircleLoadingView_ringProgressColor, Color.RED)
        progressPercent = a.getFloat(R.styleable.CircleLoadingView_progressPercent, 0.25f)
        progressStartDegree =
            a.getFloat(R.styleable.CircleLoadingView_progressStartDegree, -90f) // 时钟12点位置
        logoResId = a.getResourceId(R.styleable.CircleLoadingView_logoResId, 0)
        logoMarginBorder = a.getDimension(R.styleable.CircleLoadingView_logoMarginBorder, 0f)
        autoRotate = a.getBoolean(R.styleable.CircleLoadingView_autoRotate, true)
        isAnimating = a.getBoolean(R.styleable.CircleLoadingView_animating, false)
        progressSweepAngle = progressPercent.times(360f)
        a.recycle()
        // 解析logo成Bitmap
        logoBmp = BitmapFactory.decodeResource(resources, logoResId)
        // 抗锯齿画笔
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // 画圆弧需要的矩形
        rectF = RectF()
        // 需要重写onDraw就得调用此方法
        setWillNotDraw(false)
        // 观察宿主生命周期，防止内存泄露
        observeActivityLifecycle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "onAttachedToWindow: ")
        if (valueAnimator == null && isAnimating)
            startAnimation(true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.d(TAG, "onDetachedFromWindow: ")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            getDefaultSize(0, widthMeasureSpec),
            getDefaultSize(0, heightMeasureSpec)
        )
        // 使新的宽高 等于 旧宽高中的较小值
        val childWidthSize = measuredWidth.coerceAtMost(measuredHeight)
        val newMeasureSpec = MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY)
        super.onMeasure(newMeasureSpec, newMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
//        Log.d(TAG,"width=$width, measuredWidth=$measuredWidth, height=$height, measuredHeight=$measuredHeight")
        // 圆心坐标
        viewCenterX = width / 2
        viewCenterY = height / 2
        // 使logo尺寸适应 自定义view
        logoBmp = logoBmp?.changeBitmapSize(
            width - logoMarginBorder.toInt(),
            height - logoMarginBorder.toInt()
        ) ?: logoBmp
        // 设置 矩形位置
        rectF.set(
            ringWidth / 2,
            ringWidth / 2,
            width - ringWidth / 2,
            height - ringWidth / 2
        )
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawInnerCircle(canvas)
        drawBgRing(canvas)
        drawProgressRing(canvas)
        drawLogo(canvas)
    }

    /**
     * 画中间内圆
     */
    private fun drawInnerCircle(canvas: Canvas?) {
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.color = innerCircleColor
        canvas?.drawCircle(
            viewCenterX.toFloat(),
            viewCenterY.toFloat(),
            (viewCenterX - ringWidth) + 1,
            paint
        )
    }

    /**
     * 画中间 logo
     */
    private fun drawLogo(canvas: Canvas?) {
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.color = Color.WHITE // 随便指定一个颜色,不能为0
        logoBmp?.let {
//            Log.d(TAG, "mLogoBmp=$mLogoBmp")
            val bmpLeft = viewCenterX - it.width / 2f
            val bmpTop = viewCenterY - it.height / 2f
//            canvas?.rotate(mCanvasDegree, mViewCenterX.toFloat(), mViewCenterY.toFloat())
            canvas?.drawBitmap(it, bmpLeft, bmpTop, paint)
        }
    }

    /**
     * 画背景圆环
     */
    private fun drawBgRing(canvas: Canvas?) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = ringWidth
        paint.color = ringBgColor
        canvas?.drawArc(rectF, 0f, 360f, false, paint)
    }

    /**
     * 画进度条圆环
     *
     * @param canvas
     */
    private fun drawProgressRing(canvas: Canvas?) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = ringWidth
        paint.color = progressRingColor
        // 旋转画布
//        canvas?.rotate(mCanvasDegree, mViewCenterX.toFloat(), mViewCenterY.toFloat())
        canvas?.drawArc(rectF, progressStartDegree, progressSweepAngle, false, paint)
    }

    /**
     * 启动动画
     * @param isLoop 是否无限循环旋转
     */
    fun startAnimation(isLoop: Boolean) {
        valueAnimator = ValueAnimator.ofFloat(0f, progressPercent)
        valueAnimator?.repeatCount = if (isLoop) INFINITE else 0
        valueAnimator?.interpolator = LinearInterpolator()
        valueAnimator?.duration = 1000
        valueAnimator?.addUpdateListener { animation ->
//            Log.d(TAG, "animation.animatedValue=" + animation.animatedValue)
            val i = (animation.animatedValue.toString()).toFloat()
            // 如果是 循环旋转的 loading
            if (isLoop)
                progressStartDegree = 360f.times(i / progressPercent)
            else // 如果只是 设置进度
                progressSweepAngle = 360f.times(i)
            invalidate()
        }
        valueAnimator?.start()
    }

    /**
     * 取消动画
     *
     * 对于 Activity 不是 LifecycleOwner 时，记得在 onDestroy()方法里取消动画
     */
    fun cancelAnimation() {
        valueAnimator?.cancel()
    }

    /**
     * 设置动画是否开启
     */
    fun setAnimating(animating: Boolean) {
        if (isAnimating != animating) {
            isAnimating = animating
            if (isAnimating) {
                startAnimation(true)
            } else {
                cancelAnimation()
            }
        }
    }

    /**
     * 设置进度
     * @param progressPercent 进度百分比 0~1
     */
    fun setProgressPercent(progressPercent: Float) {
        this.progressPercent = when {
            progressPercent < 0f -> 0f
            progressPercent > 1f -> 1f
            else -> progressPercent
        }
        autoRotate = false
        startAnimation(false)
    }

    /**
     * 观察 宿主 Activity 的 生命周期，在适当时机启动和关闭动画
     */
    private fun observeActivityLifecycle() {
        if (context is LifecycleOwner) {
            (context as LifecycleOwner).lifecycle.addObserver(object : LifecycleObserver {
                //                @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
//                fun onResume() {
////                    Log.d(TAG, "onResume: ")
//                    if (autoRotate) setAnimating(true)
//                }
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
//                    Log.d(TAG, "onDestroy: ")
                    setAnimating(false)
                }
            })
        }
    }

    /**
     * 重设 Bitmap 的宽高
     */
    private fun Bitmap.changeBitmapSize(newWidth: Int, newHeight: Int): Bitmap? {
        val width = this.width
        val height = this.height
//        Log.d(TAG, "width:$width")
//        Log.d(TAG, "height:$height")
        //计算压缩的比率
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        //获取想要缩放的matrix
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        //获取新的bitmap
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
//        Log.d(TAG, "newWidth" + newBitmap.width)
//        Log.d(TAG, "newHeight" + newBitmap.height)
//        return newBitmap
    }

    companion object {
        const val TAG = "CircleLoadingView"
    }
}

//@BindingAdapter("app:animating")  // Need DataBinding supported
fun setCivValueColor(clv: CircleLoadingView, isAnimating: Boolean?) {
    isAnimating?.let {
        clv.setAnimating(isAnimating)
    }
}


