package com.example.expensemanager;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.expensemanager.databinding.ActivityDashboardBinding;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    ActivityDashboardBinding binding;
    FirebaseFirestore firebaseFirestore;
    FirebaseAuth firebaseAuth;

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
                if (firebaseAuth.getCurrentUser() == null) {
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
                } catch (Exception e) {
                    Log.e(TAG, "Error starting AddTransactionActivity", e);
                }
            }
        });

        binding.refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadData();
            }
        });

        setupRealtimeUpdates();
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
    }

    private void loadData() {
        // Reset the income and expense values before recalculating
        income = 0;
        expense = 0;

        firebaseFirestore.collection("Expense").document(firebaseAuth.getUid()).collection("Notes")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        transactionModelArrayList.clear();
                        for (DocumentSnapshot ds : task.getResult()) {
                            TransactionModel model = new TransactionModel(
                                    ds.getString("id"),
                                    ds.getString("note"),
                                    ds.getString("amount"),
                                    ds.getString("type"),
                                    ds.getString("date"));

                            int amount = Integer.parseInt(ds.getString("amount"));
                            if (ds.getString("type").equals("Expense")) {
                                expense += amount;
                            } else {
                                income += amount;
                            }
                            transactionModelArrayList.add(model);
                        }

                        // Log the recalculated values
                        Log.d(TAG, "Income: " + income + ", Expense: " + expense);

                        // Update the total income, expense, and balance
                        binding.totalIncome.setText(String.valueOf(income));
                        binding.totalExpense.setText(String.valueOf(expense));
                        binding.totalBalance.setText(String.valueOf(income - expense));

                        transactionAdapter = new TransactionAdapter(DashboardActivity.this, transactionModelArrayList);
                        binding.historyRecyclerView.setAdapter(transactionAdapter);
                        setUpGraph();
                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                    }
                });
    }

    private void setupRealtimeUpdates() {
        firebaseFirestore.collection("Expense").document(firebaseAuth.getUid()).collection("Notes")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.w(TAG, "Listen failed.", error);
                            return;
                        }

                        if (value != null) {
                            // Recalculate income and expense
                            income = 0;
                            expense = 0;
                            transactionModelArrayList.clear();

                            for (DocumentSnapshot ds : value.getDocuments()) {
                                TransactionModel model = new TransactionModel(
                                        ds.getString("id"),
                                        ds.getString("note"),
                                        ds.getString("amount"),
                                        ds.getString("type"),
                                        ds.getString("date"));

                                int amount = Integer.parseInt(ds.getString("amount"));
                                if (ds.getString("type").equals("Expense")) {
                                    expense += amount;
                                } else {
                                    income += amount;
                                }
                                transactionModelArrayList.add(model);
                            }

                            // Log the recalculated values
                            Log.d(TAG, "RealtimeUpdate - Income: " + income + ", Expense: " + expense);

                            // Update the total income, expense, and balance
                            binding.totalIncome.setText(String.valueOf(income));
                            binding.totalExpense.setText(String.valueOf(expense));
                            binding.totalBalance.setText(String.valueOf(income - expense));

                            transactionAdapter = new TransactionAdapter(DashboardActivity.this, transactionModelArrayList);
                            binding.historyRecyclerView.setAdapter(transactionAdapter);
                            setUpGraph();
                        }
                    }
                });
    }

    private void setUpGraph() {
        List<PieEntry> pieEntries = new ArrayList<>();
        List<Integer> colorsList = new ArrayList<>();
        long remainingIncome = income - expense;

        if (remainingIncome > 0) {
            pieEntries.add(new PieEntry(remainingIncome, "Balance"));
            colorsList.add(getResources().getColor(R.color.lgreen));
        }
        if (expense > 0) {
            pieEntries.add(new PieEntry(expense, "Expense"));
            colorsList.add(getResources().getColor(R.color.wred));
        }

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "Income vs Expense");
        pieDataSet.setColors(colorsList);
        PieData pieData = new PieData(pieDataSet);
        binding.pieChart.setData(pieData);
        binding.pieChart.invalidate();
    }
}