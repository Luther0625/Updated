package com.example.expensemanager;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.expensemanager.databinding.ActivityDashboardBinding;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    ActivityDashboardBinding binding;
    FirebaseFirestore firebaseFirestore;
    FirebaseAuth firebaseAuth;

    int sumExpense = 0;
    int sumIncome = 0;
    private long income = 0, expense = 0;

    ArrayList<TransactionModel> transactionModelArrayList;
    TransactionAdapter transactionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        transactionModelArrayList = new ArrayList<>();

        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.historyRecyclerView.setHasFixedSize(true);

        firebaseAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null){
                    startActivity(new Intent(DashboardActivity.this, MainActivity.class));
                    finish();
                }
            }
        });

        binding.signOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createSignOutDialog();
            }
        });

        binding.addFloatingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(DashboardActivity.this, AddTransactionActivity.class));
                }catch (Exception e){

                }
            }
        });

        binding.refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(DashboardActivity.this, DashboardActivity.class));
                    finish();
                }catch (Exception e){

                }
            }
        });
        loadData();
        setUpGraph();
    }

    private void createSignOutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
        builder.setTitle("Sign Out")
                .setMessage("Sign out?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        firebaseAuth.signOut();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadData();
        setUpGraph();
    }

    private void loadData() {
        firebaseFirestore.collection("Expense").document(firebaseAuth.getUid()).collection("Notes")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        transactionModelArrayList.clear();
                        for (DocumentSnapshot ds:task.getResult()){
                            TransactionModel model = new TransactionModel(
                                    ds.getString("id"),
                                    ds.getString("note"),
                                    ds.getString("amount"),
                                    ds.getString("type"),
                                    ds.getString("date"));

                            int amount = Integer.parseInt(ds.getString("amount"));
                            if (ds.getString("type").equals("Expense")){
                                expense += amount;
                            }
                            else {
                                income += amount;
                            }
                            transactionModelArrayList.add(model);
                        }

//                        binding.totalIncome.setText(String.valueOf(sumIncome));
//                        binding.totalExpense.setText(String.valueOf(sumExpense));
//                        binding.totalBalance.setText(String.valueOf(sumIncome-sumExpense));

                        transactionAdapter = new TransactionAdapter(DashboardActivity.this, transactionModelArrayList);

                        binding.historyRecyclerView.setAdapter(transactionAdapter);
                        setUpGraph();
                    }
                });
    }

    private void setUpGraph() {
        Intent intent = getIntent();
        String newType = intent.getStringExtra(UpdateActivity.newType);
        List<com.github.mikephil.charting.data.PieEntry> pieEntries = new ArrayList<>();
        List<Integer> colorsList = new ArrayList<>();
        if (income != 0) {
            pieEntries.add(new com.github.mikephil.charting.data.PieEntry(income, "Income"));
            colorsList.add(getResources().getColor(R.color.lgreen));
        }
        if (expense != 0) {
            pieEntries.add(new PieEntry(expense, "Expense"));
            colorsList.add(getResources().getColor(R.color.yells));
        }
        com.github.mikephil.charting.data.PieDataSet pieDataSet = new PieDataSet(pieEntries, "Income vs Expense");
        pieDataSet.setColors(colorsList);
        PieData pieData = new PieData(pieDataSet);
        binding.pieChart.setData(pieData);
        binding.pieChart.invalidate();
    }
}