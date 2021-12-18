package com.example.calculatorapp

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.room.Room
import com.example.calculatorapp.model.History
import org.w3c.dom.Text

class MainActivity : AppCompatActivity() {

    private val expressionTextView: TextView by lazy{
        findViewById(R.id.expressionTextView)
    }

    private val resultTextView: TextView by lazy{
        findViewById(R.id.resultTextView)
    }

    private val historyLayout: View by lazy{
        findViewById(R.id.historyLayout)
    }

    private val historyLinearLayout : LinearLayout by lazy{
        findViewById(R.id.historyLinearLayout)
    }

    lateinit var db: AppDatabase

    private var isOperator = false
    private var hasOperator = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder( // databaseBuilder의 첫번째 인자는 context, 두번째 인자는 class 이름, 세번째 인자는 name
            applicationContext,
            AppDatabase::class.java,
            "historyDB"
        ).build()
    }

    fun buttonClicked(v : View){
        when(v.id){
            R.id.button0 -> numberButtonClicked("0")
            R.id.button1 -> numberButtonClicked("1")
            R.id.button2 -> numberButtonClicked("2")
            R.id.button3 -> numberButtonClicked("3")
            R.id.button4 -> numberButtonClicked("4")
            R.id.button5 -> numberButtonClicked("5")
            R.id.button6 -> numberButtonClicked("6")
            R.id.button7 -> numberButtonClicked("7")
            R.id.button8 -> numberButtonClicked("8")
            R.id.button9 -> numberButtonClicked("9")
            R.id.buttonPlus -> operatorButtonClicked("+")
            R.id.buttonMinus -> operatorButtonClicked("-")
            R.id.buttonMulti -> operatorButtonClicked("*")
            R.id.buttonDivider -> operatorButtonClicked("/")
            R.id.buttonModulo -> operatorButtonClicked("%")
        }
    }

    private fun numberButtonClicked(number : String){

        if(isOperator){
            expressionTextView.append(" ")
        }

        isOperator = false

        val expressionText = expressionTextView.text.split(" ")
        if (expressionText.isNotEmpty() && expressionText.last().length >= 15){
            Toast.makeText(this, "15자리까지만 사용할 수 있습니다.",Toast.LENGTH_SHORT).show()
            return
        } else if(expressionText.last().isEmpty() && number == "0"){
            Toast.makeText(this, "0은 제일 앞에 올 수 없습니다.",Toast.LENGTH_SHORT).show()
            return
        }

        expressionTextView.append(number)

        resultTextView.text = calculateExpression()
    }

    private fun operatorButtonClicked(operator: String){
        if(expressionTextView.text.isEmpty()){
            return
        }

        when{
            isOperator -> {
                val text = expressionTextView.text.toString()
                expressionTextView.text = text.dropLast(1) + operator
            }
            hasOperator -> {
                Toast.makeText(this, "연산자는 한 번만 사용할 수 있습니다.",Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                expressionTextView.append(" $operator")
            }
        }

        val ssb = SpannableStringBuilder(expressionTextView.text) // 문자열의 일부분에 색을 입히기 위해 Spannable 사용
        ssb.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(applicationContext,R.color.green)),
            expressionTextView.text.length - 1,
            expressionTextView.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        expressionTextView.text = ssb

        isOperator = true
        hasOperator = true
    }

    fun resultButtonClicked(v : View){
        val expressionTexts = expressionTextView.text.split(" ")

        if(expressionTextView.text.isEmpty() || expressionTexts.size == 1){
            return
        }

        if(expressionTexts.size != 3 && hasOperator){
            Toast.makeText(this, "아직 완성되지 않은 수식입니다.",Toast.LENGTH_SHORT).show()
            return
        }

        if(expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()){
            Toast.makeText(this, "오류가 발생했습니다.",Toast.LENGTH_SHORT).show()
            return
        }

        val expressionText = expressionTextView.text.toString()
        val resultText = calculateExpression()

        // 네트워크 또는 디비와 관련된 작업들은 main Thread에서 실행하기 무겁기 때문에 sub thread에서 진행해야 한다.
        Thread(Runnable {
            db.historyDao().insertHistory((History(null, expressionText, resultText))) // uid 값을 null로 줘도 PrimaryKey이므로 들어갈 때마다 하나씩 올라간다.
        }).start()

        resultTextView.text = ""
        expressionTextView.text = resultText

        isOperator = false
        hasOperator = false
    }

    private fun calculateExpression(): String {
        val expressionTexts = expressionTextView.text.split(" ")
        if (hasOperator.not() || expressionTexts.size != 3){ // not 함수는 파이썬에서 not을 붙인것과 같은 효과이다.
            return ""
        } else if(expressionTexts[0].isNumber().not() || expressionTexts[2].isNumber().not()){
            return ""
        }
        val exp1 = expressionTexts[0].toBigInteger()
        val exp2 = expressionTexts[2].toBigInteger()

        return when(expressionTexts[1]){
            "+" -> (exp1 + exp2).toString()
            "-" -> (exp1 - exp2).toString()
            "*" -> (exp1 * exp2).toString()
            "/" -> (exp1 / exp2).toString()
            "%" -> (exp1 % exp2).toString()
            else -> ""
        }
    }

    fun clearButtonClicked(v : View){
        expressionTextView.text = ""
        resultTextView.text = ""
        isOperator = false
        hasOperator = false
    }

    fun historyButtonClicked(v : View){
        historyLayout.isVisible = true
        historyLinearLayout.removeAllViews() // LinearLayout 하위의 모든 View들이 삭제된다.

        Thread(Runnable {
            db.historyDao().getAll().reversed().forEach{ // 나중에 저장된게 밑으로 가기 때문에 최근 계산값을 위로 오게 하기 위해서 reverse 함수를 이용해서 리스트를 뒤집어준다.
                runOnUiThread{ // sub Thread에서 UI 자원을 다루기 위해서는 runOnUiThread 함수를 이용해야 한다.
                    val historyView = LayoutInflater.from(this).inflate(R.layout.history_row, null, false) // 두번째 인자는 root인데, historyLinearLayout을 root로 갖지만, 나중에 addView로 붙여줄것이기 때문에 지금은 null값을 줌
                    historyView.findViewById<TextView>(R.id.expressionTextView).text = it.expression
                    historyView.findViewById<TextView>(R.id.resultTextView).text = "=${it.result}"

                    historyLinearLayout.addView(historyView)
                }
            }
        }).start()
    }

    fun closeHistoryButtonClicked(v : View){
        historyLayout.isVisible = false
    }

    fun historyClearButtonClicked(v : View){
        historyLinearLayout.removeAllViews()

        Thread(Runnable{
            db.historyDao().deleteAll()
        }).start()
    }
}

fun String.isNumber(): Boolean { // String의 확장함수
    return try{
        this.toBigInteger()
        true
    } catch(e : NumberFormatException){
        false
    }
}