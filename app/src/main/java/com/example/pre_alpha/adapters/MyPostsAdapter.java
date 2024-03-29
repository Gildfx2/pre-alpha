package com.example.pre_alpha.adapters;

import static android.content.Context.MODE_PRIVATE;
import static com.example.pre_alpha.models.FBref.refPosts;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.example.pre_alpha.R;
import com.example.pre_alpha.main.CreatePostFragment;
import com.example.pre_alpha.models.Post;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;

public class MyPostsAdapter extends ArrayAdapter<PostData> {

    private FragmentManager fragmentManager;
    Post postTmp;

    public MyPostsAdapter(@NonNull Context context, ArrayList<PostData> dataArrayList, FragmentManager fragmentManager) {
        super(context, R.layout.list_ot_my_items, dataArrayList);
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        PostData postData = getItem(position);
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.list_ot_my_items, parent, false);
        }

        ImageView image = view.findViewById(R.id.my_post_image);
        TextView item = view.findViewById(R.id.my_post_item);
        TextView name = view.findViewById(R.id.my_post_name);

        if (postData != null && !postData.image.toString().isEmpty()) {
            Glide.with(getContext())
                    .load(postData.image)
                    .into(image);
        }
        else
            image.setImageResource(R.drawable.default_image);
        item.setText(postData.item);
        name.setText(postData.name);

        ImageView editButton = view.findViewById(R.id.edit_post);
        ImageView deleteButton = view.findViewById(R.id.delete_post);

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refPosts.child(postData.getPostId()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        postTmp = snapshot.getValue(Post.class);
                        moveToEditScreen();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseError", error.getMessage());
                    }
                });

            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.delete_post_confirmation_dialog);
                Button delete, undelete;
                delete=dialog.findViewById(R.id.confirm_delete_post);
                undelete=dialog.findViewById(R.id.cancel_delete_post);
                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        refPosts.child(postData.getPostId()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialog.cancel();
                                Toast.makeText(getContext(),"המודעה נמחקה בהצלחה", Toast.LENGTH_SHORT).show();
                                String storagePath = "Users_Posts_Images/";
                                StorageReference storageReference;
                                storageReference = FirebaseStorage.getInstance().getReference();
                                String filePathAndName = storagePath + "image" + "_" + postData.getPostId();
                                StorageReference storageReference2 = storageReference.child(filePathAndName);
                                if(!postData.getImage().toString().isEmpty()) {
                                    storageReference2.delete();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                dialog.cancel();
                                Toast.makeText(getContext(),"מודעה לא נמחקה, נסה שנית", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                undelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                    }
                });
                dialog.show();
            }
        });

        return view;
    }

    private String makeDateString(int day, int month, int year){
        return getMonthFormatFromString(month) + " " + day + " " + year;
    }

    private String getMonthFormatFromString(int month){
        if(month==1) return "JAN";
        if(month==2) return "FEB";
        if(month==3) return "MAR";
        if(month==4) return "APR";
        if(month==5) return "MAY";
        if(month==6) return "JUN";
        if(month==7) return "JUL";
        if(month==8) return "AUG";
        if(month==9) return "SEP";
        if(month==10) return "OCT";
        if(month==11) return "NOV";
        if(month==12) return "DEC";

        return "JAN";
    }

    private void moveToEditScreen(){
        CreatePostFragment createPostFragment = new CreatePostFragment();
        Bundle bundle = new Bundle();
        bundle.putString("state_name", postTmp.getName());
        bundle.putString("state_item", postTmp.getItem());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(postTmp.getTimeStamp());
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        bundle.putString("state_date", makeDateString(dayOfMonth, month, year));
        bundle.putString("state_image", postTmp.getImage());
        bundle.putDouble("latitude", postTmp.getLatitude());
        bundle.putDouble("longitude", postTmp.getLongitude());
        bundle.putString("state_about", postTmp.getAbout());
        bundle.putInt("state_radius", postTmp.getRadius());
        bundle.putString("state_post_id", postTmp.getPostId());
        createPostFragment.setArguments(bundle);
        SharedPreferences state = getContext().getSharedPreferences("state", MODE_PRIVATE);
        SharedPreferences.Editor editor = state.edit();
        editor.putString("state", "edit");
        editor.apply();
        editor.commit();
        fragmentManager.beginTransaction().replace(R.id.frameLayout, createPostFragment).commit();
    }
}
