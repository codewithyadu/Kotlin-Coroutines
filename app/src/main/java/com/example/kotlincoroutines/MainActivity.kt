package com.example.kotlincoroutines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.kotlincoroutines.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlin.system.measureTimeMillis

/**
 * Note :
 * Coroutine builders :
 *
 * 1.with context :- wait for result one from first coroutine then execute second coroutine
 * Blocks subsequent code inside coroutine
 *
 * 2.launch :- do not return result and used for executing parallel coroutines
 * Does not blocks subsequent code inside coroutine
 *
 * 3.async and await :- return result and used for executing parallel coroutines
 * async without await works similar to launch
 * Does not blocks subsequent code inside coroutine
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var job: CompletableJob

    companion object {
        private const val JOB_TIMEOUT = 1900L
        private const val PROGRESS_MAX = 100
        private const val PROGRESS_START = 0
        private const val JOB_TIME = 4000 //ms
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

//        binding.button.setOnClickListener {
//            CoroutineScope(IO).launch {
//                //fakeApiRequest()
//                fakeApiRequestForNetworkTimeoutExample()
//            }
//        }

        //All code from here related to job and job cancellation
//        initJob()
//        binding.button.text = "Start Job"
//        binding.button.setOnClickListener {
//            startOrCancelJob(job)
//        }

        //All code for parallel coroutine execution
        binding.button.setOnClickListener {
            //parallelApiRequest()
            parallelApiRequestUsingAsyncAndAwait()
        }

        //All code for sequential call
        binding.button.setOnClickListener {
            sequentialApiRequestUsingAsyncAndAwait()
        }

        //run blocking vs coroutine scope
        binding.button.setOnClickListener {
            runBlockingVsCoroutineScope()
        }
    }

    //async and await returns the result of the specified type
    //whereas launch executes the coroutine without returning the result
    //i.e in async and await we can have result outside the scope whereas in
    //launch we have result confined within the scope
    private fun parallelApiRequest() {
        val startTime = System.currentTimeMillis()
        val parentJob = CoroutineScope(IO).launch {
            val job1 = launch {
                Log.d("ParallelCoroutine","Start time 1 : ${System.currentTimeMillis()}")
                val time1 = measureTimeMillis {
                    Log.d("ParallelCoroutine","Job1 in thread ${Thread.currentThread().name}")
                    val result = getResultOneFromApi()
                    Log.d("ParallelCoroutine","Result1 : $result")
                }
                Log.d("ParallelCoroutine","Completed job1 in time : $time1 ms")
            }
            val job2 = launch {
                Log.d("ParallelCoroutine","Start time 2 : ${System.currentTimeMillis()}")
                val time2 = measureTimeMillis {
                    Log.d("ParallelCoroutine","Job2 in thread ${Thread.currentThread().name}")
                    val result = getResultTwoFromApi("")
                    Log.d("ParallelCoroutine","Result2 : $result")
                }
                Log.d("ParallelCoroutine","Completed job2 in time : $time2 ms")
            }
        }
        parentJob.invokeOnCompletion {
            val endTime = System.currentTimeMillis()
            val elapsedTime = endTime - startTime
            Log.d("ParallelCoroutine","Elapsed Time : $elapsedTime")
        }
    }

    //async and await returns the result of the specified type
    //whereas launch executes the coroutine without returning the result
    //i.e in async and await we can have result outside the scope whereas in
    //launch we have result confined within the scope
    private fun parallelApiRequestUsingAsyncAndAwait() {
        CoroutineScope(IO).launch {
            val elapsedTime = measureTimeMillis {
                val result1: Deferred<String> = async {
                    Log.d("ParallelCoroutine","Job1 in thread ${Thread.currentThread().name}")
                    getResultOneFromApi()
                }
                val result2: Deferred<String> = async {
                    Log.d("ParallelCoroutine","Job2 in thread ${Thread.currentThread().name}")
                    getResultTwoFromApi("")
                }

                setResult("Result1 : ${result1.await()}, Result2 : ${result2.await()}")
            }
            Log.d("ParallelCoroutine","Elapsed Time : $elapsedTime")
        }
    }

    private suspend fun setResult(result: String) {
        withContext(Main) {
            binding.textView.text = result
        }
    }

    /**
     * Here after one api request, second api request is made
     * In Coroutines second api request waits for the first api request to get completed
     * before it gets called.
     * So it means here inside suspend coroutines work in synchronous way
     */
    private suspend fun fakeApiRequest() {
        val result1 = getResultOneFromApi()
        Log.d("Coroutine","Result1: $result1")
        setResult(result1)
        val result2 = getResultTwoFromApi(result1)
        setResult(result2)
    }

    private suspend fun getResultOneFromApi(): String {
        logThread("getResultOneFromApi")
        delay(1700) //It delays a single coroutine
        return "Result1"
    }

    private suspend fun getResultTwoFromApi(result: String): String {
        logThread("getResultTwoFromApi")
        delay(1000) //It delays a single coroutine
        return "Result2"
    }

    private fun logThread(methodName: String) {
        Log.d("Coroutine","$methodName - ${Thread.currentThread().name}")
    }

    private suspend fun fakeApiRequestForNetworkTimeoutExample() {
        withContext(IO) {
            val job = withTimeoutOrNull(JOB_TIMEOUT) {
                val result1 = getResultOneFromApi()
                Log.d("Coroutine","Result1: $result1")
                setResult(result1)
                val result2 = getResultTwoFromApi(result1)
                Log.d("Coroutine","Result2: $result2")
                setResult(result2)
            }

            if (job == null) {
                setResult("TimeOut happened as job took longer than 1900ms to execute")
            }
        }
    }

    private fun initJob() {
        binding.button.text = "Start Job"
        job = Job()
        job.invokeOnCompletion {
            showToast(it?.message ?: "unknown cancellation error")
            binding.progressBar.max = PROGRESS_MAX
            binding.progressBar.progress = PROGRESS_START
        }
    }

    private fun showToast(text: String) {
        GlobalScope.launch(Main) {
            Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show()
        }
    }

    private fun startOrCancelJob(job: Job) {
        if (binding.progressBar.progress > 0) {
            resetJob()
        } else {
            binding.button.text = "Cancel Job"
            CoroutineScope(IO + job).launch {
                for (i in PROGRESS_START..PROGRESS_MAX) {
                    delay((JOB_TIME/ PROGRESS_MAX).toLong())
                    binding.progressBar.progress = i
                }
                showToast("Job is complete")
            }
        }
    }

    private fun resetJob() {
        if (job.isActive || job.isCompleted ) {
            //Cancelling the job doesn't mean our coroutine will stop the task
            //to stop the task use cancellation states to stop periodic work
            job.cancel(CancellationException("Resetting Job"))
        }
        //Need to again initialize the job because once cancelled,
        //again calling the coroutine scope will not work
        //need to again initialize the job to start the
        //coroutine again
        initJob()
    }

    //Same can be achieved using with context
    private fun sequentialApiRequestUsingAsyncAndAwait() {
        CoroutineScope(IO).launch {
            val executionTime = measureTimeMillis {
                val result1 = async {
                    Log.d("SequentialCoroutine","Job1 in thread ${Thread.currentThread().name}")
                    getResultOneFromApi()
                }.await()

                val result2 = async {
                    Log.d("SequentialCoroutine","Job2 in thread ${Thread.currentThread().name}")
                    getResultTwoFromApi(result1)
                }.await()

                Log.d("SequentialCoroutine","Result2 : $result2")
            }

            Log.d("SequentialCoroutine","Total elapsed time : $executionTime")
        }
    }

    /**
     * runBlocking executes the current job inside the thread
     * and blocks the current thread
     *
     * Here in this example both the threads are launched
     * at same time, however after 1 sec delay
     * when runBlocking is executed it blocks the current
     * thread, so all other results are printed only after
     * run blocking is finished as per the mentioned delay
     * of 4s
     *
     * This is the major difference between run blocking and
     * CoroutineScope.
     * run blocking blocks the main thread until its completed
     * whereas the coroutine scope doesn't block and execute
     * many jobs within the same thread without blocking
     * the thread
     */
    private fun runBlockingVsCoroutineScope() {
        CoroutineScope(Main).launch {
            Log.d("Coroutine","Job1 in thread ${Thread.currentThread().name}")
            val result1 = getResultOneFromApi()
            Log.d("Coroutine","Result1 : $result1")

            val result2 = getResultOneFromApi()
            Log.d("Coroutine","Result2 : $result2")

            val result3 = getResultOneFromApi()
            Log.d("Coroutine","Result3 : $result3")

            val result4 = getResultOneFromApi()
            Log.d("Coroutine","Result4 : $result4")

            val result5 = getResultOneFromApi()
            Log.d("Coroutine","Result5 : $result5")
        }

        CoroutineScope(Main).launch {
            delay(1000)
            runBlocking {
                Log.d("Coroutine","Blocking Thread : ${Thread.currentThread().name}")
                delay(5000)
                Log.d("Coroutine","Done Blocking Thread : ${Thread.currentThread().name}")
            }
        }
    }
}