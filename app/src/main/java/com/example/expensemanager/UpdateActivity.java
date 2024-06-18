package com.example.expensemanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.expensemanager.databinding.ActivityUpdateBinding;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UpdateActivity extends AppCompatActivity {
    ActivityUpdateBinding binding;
    public static String newType = "000";

    FirebaseFirestore firebaseFirestore;
    FirebaseAuth firebaseAuth;
    TransactionAdapter transactionAdapter;
    ArrayList<TransactionModel> transactionModelArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        transactionModelArrayList = new ArrayList<>();


        String id = getIntent().getStringExtra("id");
        String amount = getIntent().getStringExtra("amount");
        String note = getIntent().getStringExtra("note");
        String type = getIntent().getStringExtra("type");

        binding.userAmountAdd.setText(amount);
        binding.userNoteAdd.setText(note);

        switch (type){
            case "Income":
                newType = "Income";
                binding.incomeCheckBox.setChecked(true);
                break;
            case "Expense":
                newType = "Expense";
                binding.expenseCheckBox.setChecked(true);
                break;
        }

        binding.incomeCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newType = "Income";
                binding.incomeCheckBox.setChecked(true);
                binding.expenseCheckBox.setChecked(false);
            }
        });

        binding.expenseCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newType = "Expense";
                binding.incomeCheckBox.setChecked(false);
                binding.expenseCheckBox.setChecked(true);
            }
        });

        binding.btnUpdateTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amount = binding.userAmountAdd.getText().toString();
                String note = binding.userNoteAdd.getText().toString();

//                SimpleDateFormat sdf = new SimpleDateFormat("dd MM yyyy_HH:mm", Locale.getDefault());
//                String currentDateandTime = sdf.format(new Date());
//
//                Map<String, Object> transaction = new HashMap<>();
//                transaction.put("amount", amount);
//                transaction.put("note", note);
//                transaction.put("type", newType);
//                transaction.put("date", currentDateandTime);
//
//                firebaseFirestore.collection("Expense").document(firebaseAuth.getUid()).collection("Notes").document(id)
//                        .update(transaction)
//                        .addOnSuccessListener(new OnSuccessListener<Void>() {
//                            @Override
//                            public void onSuccess(Void unused) {
//                                onBackPressed();
//                                transactionAdapter = new TransactionAdapter(UpdateActivity.this, transactionModelArrayList);
//                                Toast.makeText(UpdateActivity.this, "Added!", Toast.LENGTH_SHORT).show();
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Toast.makeText(UpdateActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
//                            }
//                        });

                firebaseFirestore.collection("Expense").document(firebaseAuth.getUid())
                        .collection("Notes").document(id)
                        .update("amount", amount, "note", note, "type", newType)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                startActivity(new Intent(UpdateActivity.this, DashboardActivity.class));
                                transactionAdapter = new TransactionAdapter(UpdateActivity.this, transactionModelArrayList);
                                Toast.makeText(UpdateActivity.this, "Update success!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(UpdateActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        binding.btnDeleteTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseFirestore.collection("Expense").document(firebaseAuth.getUid())
                        .collection("Notes")
                        .document(id).delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                startActivity(new Intent(UpdateActivity.this, DashboardActivity.class));
                                transactionAdapter = new TransactionAdapter(UpdateActivity.this, transactionModelArrayList);
                                Toast.makeText(UpdateActivity.this, "Delete successful!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(UpdateActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
}