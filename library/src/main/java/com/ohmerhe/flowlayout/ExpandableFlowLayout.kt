package com.ohmerhe.flowlayout

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.view.MotionEventCompat
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import java.util.*

class ExpandableFlowLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : ViewGroup(context, attrs) {
    private val paint: Paint

    private var mFlow = DEFAULT_FLOW
    private var mChildSpacing = DEFAULT_CHILD_SPACING
    private var mChildSpacingForLastRow = DEFAULT_CHILD_SPACING_FOR_LAST_ROW
    private var mRowSpacing = DEFAULT_ROW_SPACING
    private var mAdjustedRowSpacing = DEFAULT_ROW_SPACING
    private var mHGravity = DEFAULT_GRAVITY
    var maxRows = DEFAULT_MAX_ROWS
        private set
    private var mIsExpanded = false
    private val mExpandHeight = 16.0f
    private var mActivePointerId: Int = 0
    private var mInitialMotionX: Float = 0f
    private var mInitialMotionY: Float = 0f
    var isSupportExpanded = true
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var listener: OnExpandableListener? = null

    private val mHorizontalSpacingForRow = ArrayList<Float>()
    private val mWidthForRow = ArrayList<Int>()
    private val mHeightForRow = ArrayList<Int>()
    private val mChildNumForRow = ArrayList<Int>()
    private var mDisplayChildCount: Int = 0

    init {

        val a = context.theme.obtainStyledAttributes(
                attrs, R.styleable.ExpandableFlowLayout, 0, 0)
        try {
            mFlow = a.getBoolean(R.styleable.ExpandableFlowLayout_flow, DEFAULT_FLOW)
            try {
                mChildSpacing = a.getInt(R.styleable.ExpandableFlowLayout_childSpacing, DEFAULT_CHILD_SPACING)
            } catch (e: NumberFormatException) {
                mChildSpacing = a.getDimensionPixelSize(R.styleable.ExpandableFlowLayout_childSpacing, dpToPx(DEFAULT_CHILD_SPACING.toFloat()).toInt())
            }

            try {
                mChildSpacingForLastRow = a.getInt(R.styleable.ExpandableFlowLayout_childSpacingForLastRow, SPACING_UNDEFINED)
            } catch (e: NumberFormatException) {
                mChildSpacingForLastRow = a.getDimensionPixelSize(R.styleable.ExpandableFlowLayout_childSpacingForLastRow, dpToPx(DEFAULT_CHILD_SPACING.toFloat()).toInt())
            }

            try {
                mRowSpacing = a.getInt(R.styleable.ExpandableFlowLayout_rowSpacing, 0).toFloat()
            } catch (e: NumberFormatException) {
                mRowSpacing = a.getDimension(R.styleable.ExpandableFlowLayout_rowSpacing, dpToPx(DEFAULT_ROW_SPACING))
            }

            maxRows = a.getInt(R.styleable.ExpandableFlowLayout_maxRows, DEFAULT_MAX_ROWS)
            isSupportExpanded = a.getBoolean(R.styleable.ExpandableFlowLayout_supportExpand, true)
            mHGravity = a.getInteger(R.styleable.ExpandableFlowLayout_hGravity, DEFAULT_GRAVITY)
        } finally {
            a.recycle()
        }
        setWillNotDraw(false)
        paint = Paint()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)

        mHorizontalSpacingForRow.clear()
        mChildNumForRow.clear()
        mWidthForRow.clear()
        mHeightForRow.clear()
        mDisplayChildCount = 0

        var measuredHeight = 0
        var measuredWidth = 0
        val childCount = childCount
        var rowWidth = 0
        var maxChildHeightInRow = 0
        var childNumInRow = 0
        val rowSize = widthSize - paddingLeft - paddingRight
        val allowFlow = widthMode != View.MeasureSpec.UNSPECIFIED && mFlow
        val childSpacing = if (mChildSpacing == SPACING_AUTO && widthMode == View.MeasureSpec.UNSPECIFIED)
            0
        else
            mChildSpacing
        val tmpSpacing = (if (childSpacing == SPACING_AUTO) 0 else childSpacing).toFloat()

        for (i in 0..childCount - 1) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) {
                continue
            }

            val childParams = child.layoutParams
            var horizontalMargin = 0
            var verticalMargin = 0
            if (childParams is ViewGroup.MarginLayoutParams) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, measuredHeight)
                val marginParams = childParams
                horizontalMargin = marginParams.leftMargin + marginParams.rightMargin
                verticalMargin = marginParams.topMargin + marginParams.bottomMargin
            } else {
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
            }

            val childWidth = child.measuredWidth + horizontalMargin
            val childHeight = child.measuredHeight + verticalMargin
            if (allowFlow && rowWidth + childWidth > rowSize) { // Need flow to next row
                // Save parameters for current row
                val exactSpacingForRow = getSpacingForRow(childSpacing, rowSize, rowWidth - tmpSpacing.toInt(), childNumInRow)
                val exactRowWidth = rowWidth - tmpSpacing.toInt() * childNumInRow + exactSpacingForRow *
                        (childNumInRow -1)
                mHorizontalSpacingForRow.add(exactSpacingForRow)
                mChildNumForRow.add(childNumInRow)
                mWidthForRow.add(exactRowWidth.toInt())
                mHeightForRow.add(maxChildHeightInRow)
                if (mHorizontalSpacingForRow.size <= maxRows || mIsExpanded) {
                    measuredHeight += maxChildHeightInRow
                    mDisplayChildCount += childNumInRow
                }
                measuredWidth = max(measuredWidth, rowWidth)

                // Place the child view to next row
                childNumInRow = 1
                rowWidth = childWidth + tmpSpacing.toInt()
                maxChildHeightInRow = childHeight
            } else {
                childNumInRow++
                rowWidth += (childWidth + tmpSpacing).toInt()
                maxChildHeightInRow = max(maxChildHeightInRow, childHeight)
            }
        }

        // Measure remaining child views in the last row
        val exactSpacingForRow = if (mChildSpacingForLastRow == SPACING_ALIGN) {
            // For SPACING_ALIGN, use the same spacing from the row above if there is more than one
            // row.
            if (mHorizontalSpacingForRow.size >= 1) {
                mHorizontalSpacingForRow[mHorizontalSpacingForRow.size - 1]
            } else {
                getSpacingForRow(childSpacing, rowSize, rowWidth, childNumInRow)
            }
        } else if (mChildSpacingForLastRow != SPACING_UNDEFINED) {
            // For SPACING_AUTO and specific DP values, apply them to the spacing strategy.
            getSpacingForRow(mChildSpacingForLastRow, rowSize, rowWidth, childNumInRow)
        } else {
            // For SPACING_UNDEFINED, apply childSpacing to the spacing strategy for the last row.
            getSpacingForRow(childSpacing, rowSize, rowWidth, childNumInRow)
        }
        val exactRowWidth = rowWidth - tmpSpacing.toInt() * childNumInRow + exactSpacingForRow *
                (childNumInRow -1)
        mHorizontalSpacingForRow.add(exactSpacingForRow)
        mChildNumForRow.add(childNumInRow)
        mWidthForRow.add(exactRowWidth.toInt())
        mHeightForRow.add(maxChildHeightInRow)
        if (mHorizontalSpacingForRow.size <= maxRows || mIsExpanded) {
            measuredHeight += maxChildHeightInRow
            mDisplayChildCount += childNumInRow
        }
        measuredWidth = max(measuredWidth, rowWidth)

        if (childSpacing == SPACING_AUTO) {
            measuredWidth = widthSize
        } else if (widthMode == View.MeasureSpec.UNSPECIFIED) {
            measuredWidth = measuredWidth + paddingLeft + paddingRight
        } else {
            measuredWidth = min(measuredWidth + paddingLeft + paddingRight, widthSize)
        }

        measuredHeight += paddingTop + paddingBottom
        var rowNum = min(mHorizontalSpacingForRow.size, maxRows)
        if (mIsExpanded) {
            rowNum = mHorizontalSpacingForRow.size
        }
        val rowSpacing: Float = if (mRowSpacing == SPACING_AUTO.toFloat() && heightMode == View.MeasureSpec.UNSPECIFIED)
            0.0f
        else
            mRowSpacing
        if (rowSpacing == SPACING_AUTO.toFloat()) {
            if (rowNum > 1) {
                mAdjustedRowSpacing = ((heightSize - measuredHeight) / (rowNum - 1)).toFloat()
            } else {
                mAdjustedRowSpacing = 0f
            }
            measuredHeight = heightSize
        } else {
            mAdjustedRowSpacing = rowSpacing
            if (heightMode == View.MeasureSpec.UNSPECIFIED) {
                measuredHeight = (measuredHeight + mAdjustedRowSpacing * (rowNum - 1)).toInt()
            } else {
                measuredHeight = min(
                        (measuredHeight + mAdjustedRowSpacing * (rowNum - 1)).toInt(), heightSize)
            }
        }

        if (isDrawExpandedView) {
            val expandHeight = dpToPx(mExpandHeight).toInt()
            measuredHeight += expandHeight
            Log.d("ExpandableFlowLayout", measuredWidth.toString() + " - " + measuredHeight)
        }
        measuredWidth = if (widthMode == View.MeasureSpec.EXACTLY) widthSize else measuredWidth
        measuredHeight = if (heightMode == View.MeasureSpec.EXACTLY) heightSize else measuredHeight
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private val isDrawExpandedView: Boolean
        get() = isSupportExpanded && isNeedExpand

    private val isNeedExpand: Boolean
        get() = maxRows < mChildNumForRow.size

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight
        val paddingTop = paddingTop

        var rowCount = min(maxRows, mChildNumForRow.size)
        var childIdx = 0
        if (mIsExpanded) {
            rowCount = mChildNumForRow.size
        }
        var y = paddingTop
        for (row in 0..rowCount - 1) {
            val childNum = mChildNumForRow[row]
            val rowWidth = mWidthForRow[row]
            val rowHeight = mHeightForRow[row]
            val spacing = mHorizontalSpacingForRow[row]
            val rowSize = measuredWidth - paddingLeft - paddingRight
            var i = 0
            var x: Int= when (mHGravity){
                GRAVITY_LEFT -> paddingLeft
                GRAVITY_CENTER -> {
                    paddingLeft + (rowSize - rowWidth) / 2
                }
                GRAVITY_RIGHT -> width - paddingRight
                else -> paddingLeft
            }
            while (i < childNum && childIdx < childCount) {
                val child = getChildAt(childIdx++)
                if (child.visibility == View.GONE) {
                    continue
                } else {
                    i++
                }

                val childParams = child.layoutParams
                var marginLeft = 0
                var marginTop = 0
                var marginRight = 0
                if (childParams is ViewGroup.MarginLayoutParams) {
                    val marginParams = childParams
                    marginLeft = marginParams.leftMargin
                    marginRight = marginParams.rightMargin
                    marginTop = marginParams.topMargin
                }

                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight
                when (mHGravity) {
                    GRAVITY_LEFT -> {
                        child.layout(x + marginLeft, y + marginTop,
                                x + marginLeft + childWidth, y + marginTop + childHeight)
                        x += (childWidth.toFloat() + spacing + marginLeft.toFloat() + marginRight.toFloat()).toInt()
                    }
                    GRAVITY_CENTER -> {
                        child.layout(x + marginLeft, y + marginTop,
                                x + marginLeft + childWidth, y + marginTop + childHeight)
                        x += (childWidth.toFloat() + spacing + marginLeft.toFloat() + marginRight.toFloat()).toInt()
                    }
                    GRAVITY_RIGHT -> {
                        child.layout(x - marginRight - childWidth, y + marginTop,
                                x - marginRight, y + marginTop + childHeight)
                        x -= (childWidth.toFloat() + spacing + marginLeft.toFloat() + marginRight.toFloat()).toInt()
                    }
                }
            }
            y += (rowHeight + mAdjustedRowSpacing).toInt()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isDrawExpandedView) return
        val res = resources
        val bitmap = BitmapFactory.decodeResource(res, if (mIsExpanded)
            R.drawable.flowlayout_fold
        else
            R.drawable.flowlayout_expand)
        //        Log.d("ExpandableFlowLayout", getMeasuredHeight() - (int) (dpToPx(mExpandHeight)) + " - " + getMeasuredHeight());
        //        Rect r = new Rect(0, getMeasuredHeight() - (int) (dpToPx(mExpandHeight)), getMeasuredWidth(), getMeasuredHeight());
        //        // fill
        //        paint.setStyle(Paint.Style.FILL);
        //        paint.setColor(Color.MAGENTA);
        //        canvas.drawRect(r, paint);
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        val expandHeight = dpToPx(mExpandHeight).toInt()
        canvas.drawBitmap(bitmap, ((measuredWidth - bitmapWidth) / 2).toFloat(), (measuredHeight - expandHeight + (expandHeight - bitmapHeight) / 2).toFloat(), paint)
        bitmap.recycle()
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        if (indexOfChild(child) < mDisplayChildCount || mIsExpanded) {
            return super.drawChild(canvas, child, drawingTime)
        }
        return true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isDrawExpandedView){
            return super.onInterceptTouchEvent(ev)
        }
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                val activePointerId = MotionEventCompat.getPointerId(ev, 0)
                val x = ev.getX(activePointerId)
                val y = ev.getY(activePointerId)
                return x > 0 && x < width && y > height - dpToPx(mExpandHeight) && y < height
            }
            MotionEvent.ACTION_UP -> {
            }
            MotionEvent.ACTION_CANCEL -> {
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isDrawExpandedView) {
            return super.onTouchEvent(ev)
        }
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0)
                mInitialMotionX = ev.getX(mActivePointerId)
                mInitialMotionY = ev.getY(mActivePointerId)
                return mInitialMotionX > 0 && mInitialMotionX < width
                        && mInitialMotionY > height - dpToPx(mExpandHeight) && mInitialMotionY < height
            }
            MotionEvent.ACTION_UP -> {
                val x = this.getMotionEventX(ev, this.mActivePointerId)
                val xDiff = Math.abs(x - this.mInitialMotionX)
                val y = this.getMotionEventY(ev, this.mActivePointerId)
                val yDiff = Math.abs(y - this.mInitialMotionY)
                if (xDiff <= 10.0f && yDiff <= 10.0f) {
                    // 不是移动，触发点击事件
                    performExpandClick()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
            }
        }
        return super.onTouchEvent(ev)
    }

    private fun performExpandClick() {
        mIsExpanded = !mIsExpanded
        listener?.onExpandViewClick(mIsExpanded)
        requestLayout()
        invalidate()
    }

    private fun getMotionEventX(ev: MotionEvent, activePointerId: Int): Float {
        val index = MotionEventCompat.findPointerIndex(ev, activePointerId)
        return if (index < 0) -1.0f else MotionEventCompat.getX(ev, index)
    }

    private fun getMotionEventY(ev: MotionEvent, activePointerId: Int): Float {
        val index = MotionEventCompat.findPointerIndex(ev, activePointerId)
        return if (index < 0) -1.0f else MotionEventCompat.getY(ev, index)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): ViewGroup.LayoutParams {
        return ViewGroup.MarginLayoutParams(p)
    }

    override fun generateLayoutParams(attrs: AttributeSet): ViewGroup.LayoutParams {
        return ViewGroup.MarginLayoutParams(context, attrs)
    }

    /**
     * Returns whether to allow child views flow to next row when there is no enough space.

     * @return Whether to flow child views to next row when there is no enough space.
     */
    /**
     * Sets whether to allow child views flow to next row when there is no enough space.

     * @param flow true to allow flow. false to restrict all child views in one row.
     */
    var isFlow: Boolean
        get() = mFlow
        set(flow) {
            mFlow = flow
            requestLayout()
        }

    /**
     * Returns the horizontal spacing between child views.

     * @return The spacing, either [ExpandableFlowLayout.SPACING_AUTO], or a fixed size in pixels.
     */
    /**
     * Sets the horizontal spacing between child views.

     * @param childSpacing The spacing, either [ExpandableFlowLayout.SPACING_AUTO], or a fixed size in
     * *        pixels.
     */
    var childSpacing: Int
        get() = mChildSpacing
        set(childSpacing) {
            mChildSpacing = childSpacing
            requestLayout()
        }

    /**
     * Returns the horizontal spacing between child views of the last row.

     * @return The spacing, either [ExpandableFlowLayout.SPACING_AUTO],
     * *         [ExpandableFlowLayout.SPACING_ALIGN], or a fixed size in pixels
     */
    /**
     * Sets the horizontal spacing between child views of the last row.

     * @param childSpacingForLastRow The spacing, either [ExpandableFlowLayout.SPACING_AUTO],
     * *        [ExpandableFlowLayout.SPACING_ALIGN], or a fixed size in pixels
     */
    var childSpacingForLastRow: Int
        get() = mChildSpacingForLastRow
        set(childSpacingForLastRow) {
            mChildSpacingForLastRow = childSpacingForLastRow
            requestLayout()
        }

    /**
     * Returns the vertical spacing between rows.

     * @return The spacing, either [ExpandableFlowLayout.SPACING_AUTO], or a fixed size in pixels.
     */
    /**
     * Sets the vertical spacing between rows in pixels. Use SPACING_AUTO to evenly place all rows
     * in vertical.

     * @param rowSpacing The spacing, either [ExpandableFlowLayout.SPACING_AUTO], or a fixed size in
     * *        pixels.
     */
    var rowSpacing: Float
        get() = mRowSpacing
        set(rowSpacing) {
            mRowSpacing = rowSpacing
            requestLayout()
        }

    fun maxRows(maxRows: Int) {
        this.maxRows = maxRows
        requestLayout()
    }

    private fun max(a: Int, b: Int): Int {
        return if (a > b) a else b
    }

    private fun min(a: Int, b: Int): Int {
        return if (a < b) a else b
    }

    private fun getSpacingForRow(spacingAttribute: Int, rowSize: Int, usedSize: Int, childNum: Int): Float {
        val spacing: Float
        if (spacingAttribute == SPACING_AUTO) {
            if (childNum > 1) {
                spacing = ((rowSize - usedSize) / (childNum - 1)).toFloat()
            } else {
                spacing = 0f
            }
        } else {
            spacing = spacingAttribute.toFloat()
        }
        return spacing
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    companion object {
        private val LOG_TAG = ExpandableFlowLayout::class.java!!.getSimpleName()

        /**
         * Special value for the child view spacing.
         * SPACING_AUTO means that the actual spacing is calculated according to the size of the
         * container and the number of the child views, so that the child views are placed evenly in
         * the container.
         */
        val SPACING_AUTO = -65536

        /**
         * Special value for the horizontal spacing of the child views in the last row
         * SPACING_ALIGN means that the horizontal spacing of the child views in the last row keeps
         * the same with the spacing used in the row above. If there is only one row, this value is
         * ignored and the spacing will be calculated according to childSpacing.
         */
        val SPACING_ALIGN = -65537

        private val GRAVITY_LEFT = 0
        private val GRAVITY_RIGHT = 4
        private val GRAVITY_CENTER = 8

        private val SPACING_UNDEFINED = -65538

        private val DEFAULT_FLOW = true
        private val DEFAULT_CHILD_SPACING = 0
        private val DEFAULT_CHILD_SPACING_FOR_LAST_ROW = SPACING_UNDEFINED
        private val DEFAULT_ROW_SPACING = 0f
        private val DEFAULT_GRAVITY = GRAVITY_LEFT
        private val DEFAULT_MAX_ROWS = Integer.MAX_VALUE
    }
}

interface OnExpandableListener{
    fun onExpandViewClick(isExpand: Boolean)
}
