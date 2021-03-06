package feri.itk.pojejinpovej.UI.Fragments


import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionInflater
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import feri.itk.pojejinpovej.Data.Models.Restaurant
import feri.itk.pojejinpovej.Data.Models.Review
import feri.itk.pojejinpovej.Data.ViewModels.RestaurantDetailsViewModel
import feri.itk.pojejinpovej.R
import feri.itk.pojejinpovej.UI.Adapters.RestaurantReviewsAdapter
import feri.itk.pojejinpovej.Util.PicassoImageLoader
import feri.itk.pojejinpovej.Util.RecyclerViewItemDecoration
import kotlinx.android.synthetic.main.fragment_main_screen.*
import kotlinx.android.synthetic.main.fragment_restaurant_details.*
import kotlinx.android.synthetic.main.review_alert.view.*
import kotlinx.android.synthetic.main.suggestion_recycler_row.view.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass.
 */
class RestaurantDetails : Fragment() {

    lateinit var restaurantViewModel: RestaurantDetailsViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mCurrentLocation: Location
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        //transition is postponed to give picasso time to load the picture
        postponeEnterTransition()
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_restaurant_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //initiating lateinit var restaurantViewModel which contains data for the restaurant
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)
        restaurantViewModel = ViewModelProviders.of(activity!!)[RestaurantDetailsViewModel::class.java]
        restaurantViewModel.getRestaurant().observe(this, Observer { restaurant ->
            loadRestaurantData(restaurant)
        })
        restaurant_reviews_recycler_view.addItemDecoration(
            RecyclerViewItemDecoration(
                resources.getDimension(R.dimen.search_results_row_padding).toInt()
            )
        )
        addReviewFAB.setOnClickListener {
            openReviewAlert()
        }

        firebaseAuth = FirebaseAuth.getInstance()

        if(firebaseAuth.currentUser==null){
            addReviewFAB.visibility=View.INVISIBLE
        }

        /*fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            mCurrentLocation = location!!
        }*/

    }

    fun openWindow(urls: String) {
        val uris = Uri.parse(urls)
        val intents = Intent(Intent.ACTION_VIEW, uris)
        requireActivity().startActivity(intents)
    }

    private fun loadRestaurantData(restaurant: Restaurant){
        loadRestaurantPicture(restaurant.picture)
        details_restaurant_name.text = restaurant.name
        details_restaurant_address.text = restaurant.address
        details_restaurant_description.text = restaurant.description
        details_restaurant_price.text = restaurant.price.toString()
        details_restaurant_rating.text = restaurant.rate.toString()
        details_restaurant_working_hours.text = restaurant.workingHours.workingHoursToday()
        details_restaurant_distance.text = restaurant.distance.toString()+"m"

        details_restaurant_food_rating_stars.rating = restaurant.rateFood.toFloat()
        details_restaurant_offer_rating_stars.rating = restaurant.rateOffer.toFloat()
        details_restaurant_service_rating_stars.rating = restaurant.rateService.toFloat()
        details_restaurant_star_icon.rating = restaurant.rate.toFloat()
        if(!restaurant.isOpenNow()){
            details_restaurant_open_indicator.visibility = View.INVISIBLE
            details_restaurant_closed_indicator.visibility = View.VISIBLE
        }
        else{
            details_restaurant_open_indicator.visibility = View.VISIBLE
            details_restaurant_closed_indicator.visibility = View.INVISIBLE
        }

        setupReviewsList(restaurant.reviews)


        restaurant_details_web.setOnClickListener{
            openWindow(restaurant.website)
        }
    }

    private fun loadRestaurantPicture(picture: String) {
        try{
            val picasso = Picasso.get()
            if(picture.isEmpty()){
                picasso
                    .load(R.drawable.app_logo_transparent)
                    .fit()
                    .centerCrop()
                    .into(details_restaurant_picture)
                startPostponedEnterTransition()
            }
            else{
                picasso
                    .load(picture)
                    .fit()
                    .centerCrop()
                    .into(details_restaurant_picture, object: com.squareup.picasso.Callback{
                        override fun onSuccess() {
                            startPostponedEnterTransition()
                        }

                        override fun onError(e: Exception?) {
                            Toast.makeText(context, R.string.picture_loading_error, Toast.LENGTH_SHORT).show()
                            Log.i("picasso", e.toString())
                            picasso
                                .load(R.drawable.app_logo_transparent)
                                .fit()
                                .centerCrop()
                                .into(details_restaurant_picture)
                            startPostponedEnterTransition()
                        }

                    })
            }
        }
        catch(e: Exception){
            Toast.makeText(context, R.string.picture_loading_error, Toast.LENGTH_SHORT).show()
        }

    }
    private fun setupReviewsList(reviews: ArrayList<Review>) {
        val reviewsAdapter =
            RestaurantReviewsAdapter(reviews,{review: Review -> reviewLikeButtonClicked(review)}, {review: Review -> reviewDislikeButtonClicked(review)})
        val restaurantReviewsRecycler = restaurant_reviews_recycler_view
        val layoutManager = LinearLayoutManager(context)
        restaurantReviewsRecycler.layoutManager = layoutManager
        restaurantReviewsRecycler.adapter = reviewsAdapter
    }
    private fun reviewLikeButtonClicked(review: Review){
        restaurantViewModel.reviewLiked(review)
    }
    private fun reviewDislikeButtonClicked(review: Review){
        restaurantViewModel.reviewDisliked(review)
    }
    private fun openReviewAlert(){
        val alertView = LayoutInflater.from(context).inflate(R.layout.review_alert, view as ViewGroup, false)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.rate_restaurant_title)
            .setMessage(resources.getString(R.string.rate_restaurant_text))
            .setNegativeButton(resources.getString(R.string.cancel), null)
            .setPositiveButton(resources.getString(R.string.confirm)){ _, _ ->
                val food = alertView.review_alert_food_rating.rating
                val offer = alertView.review_alert_offer_rating.rating
                val service = alertView.review_alert_service_rating.rating
                val reviewText = alertView.review_input.text.toString()
                restaurantViewModel.addReview(food, offer, service, reviewText)
                Toast.makeText(context, R.string.review_sent, Toast.LENGTH_SHORT).show()
                Log.i("review", reviewText)
            }
            .setOnDismissListener {
                val parent = alertView.parent as ViewGroup
                parent.removeView(alertView)
            }
            .setView(alertView)
            .show()

    }

}
