package com.personal.hubal.todolist.ui;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.personal.hubal.todolist.R;
import com.personal.hubal.todolist.models.TodoTask;

public class FirebaseTaskViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = "FirebaseTaskViewHolder";

    private TextView mTaskTitleView;
    private TextView mTaskContentView;
    private TodoTask mTask;
    private View mView;

    public ImageView mTaskReorder;

    public FirebaseTaskViewHolder(View v) {
        super(v);
        mView = v;
    }

    public void bindTodoTask(TodoTask task) {
        Log.d(TAG, "bindTodoTask");

        mTask = task;
        mTaskReorder = (ImageView) mView.findViewById(R.id.task_reorder);

        mTaskTitleView = (TextView) mView.findViewById(R.id.task_title);
        mTaskTitleView.setText(task.getTitle());

        mTaskContentView = (TextView) mView.findViewById(R.id.task_content);
        mTaskContentView.setText(task.getContent());
    }

    public TodoTask getTodoTask() {
        Log.d(TAG, "getTodoTask");
        return mTask;
    }
}