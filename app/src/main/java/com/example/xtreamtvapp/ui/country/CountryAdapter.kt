package com.example.xtreamtvapp.ui.country

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xtreamtvapp.R

/**
 * Adapter for a grid of country checkboxes. Tracks selected state.
 */
class CountryAdapter(
    private var countries: List<String>,
    initialSelected: Set<String>? = null
) : RecyclerView.Adapter<CountryAdapter.ViewHolder>() {

    private val selected = (initialSelected ?: countries.toSet()).toMutableSet()

    fun setCountries(newCountries: List<String>) {
        countries = newCountries
        selected.retainAll(countries.toSet())
        selected.addAll(countries)
        notifyDataSetChanged()
    }

    fun getSelectedCountries(): List<String> = countries.filter { it in selected }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_country_checkbox, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val country = countries[position]
        holder.bind(country, country in selected) { checked ->
            if (checked) selected.add(country) else selected.remove(country)
        }
    }

    override fun getItemCount(): Int = countries.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val check = itemView.findViewById<CheckBox>(R.id.checkCountry)
        private val label = itemView.findViewById<TextView>(R.id.countryLabel)

        fun bind(country: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
            label.text = country
            check.isChecked = checked
            check.setOnClickListener { onChecked(check.isChecked) }
            itemView.setOnClickListener {
                check.toggle()
                onChecked(check.isChecked)
            }
        }
    }
}
