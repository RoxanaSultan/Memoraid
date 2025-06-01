package com.roxanasultan.memoraid.caretaker.adapters

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient

class PlaceAutocompleteAdapter(
    context: Context,
    private val placesClient: PlacesClient
) : ArrayAdapter<AutocompletePrediction>(context, android.R.layout.simple_dropdown_item_1line) {

    private val predictions = mutableListOf<AutocompletePrediction>()

    override fun getCount(): Int = predictions.size

    override fun getItem(position: Int): AutocompletePrediction = predictions[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        view.text = getItem(position).getFullText(null).toString()
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()

                Log.d("DEBUG", "performFiltering called with query: $constraint")

                if (constraint != null && constraint.length >= 2) {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(constraint.toString())
                        .build()

                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            predictions.clear()
                            predictions.addAll(response.autocompletePredictions)

                            Log.d("DEBUG", "Received ${predictions.size} predictions")

                            results.values = predictions
                            results.count = predictions.size

                            notifyDataSetChanged() // Forțează UI-ul să se actualizeze
                        }
                        .addOnFailureListener { e ->
                            Log.e("DEBUG", "Error fetching predictions: ${e.localizedMessage}")
                        }
                }

                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return if (resultValue is AutocompletePrediction) {
                    resultValue.getFullText(null).toString()
                } else {
                    super.convertResultToString(resultValue)
                }
            }
        }
    }
}