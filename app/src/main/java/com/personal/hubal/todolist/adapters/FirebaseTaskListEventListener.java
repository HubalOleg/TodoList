package com.personal.hubal.todolist.adapters;

import android.support.v7.widget.RecyclerView;

public interface FirebaseTaskListEventListener {
    void onStartDrag(RecyclerView.ViewHolder viewHolder);
    void onClickItem(RecyclerView.ViewHolder viewHolder);
    void onAddItem();
    void onItemDismiss();
    void onPopulateViewHolder();
}
