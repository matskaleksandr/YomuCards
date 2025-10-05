import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.QuQ.yomucards.R
import com.QuQ.yomucards.TopLearner
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView

class TopLearnerAdapter : RecyclerView.Adapter<TopLearnerAdapter.TopLearnerViewHolder>() {

    private var learners: List<TopLearner> = emptyList()

    inner class TopLearnerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ShapeableImageView = itemView.findViewById(R.id.topLearnerAvatar)
        val name: TextView = itemView.findViewById(R.id.topLearnerName)
        val lessons: TextView = itemView.findViewById(R.id.topLearnerLessons)
        val number: TextView = itemView.findViewById(R.id.topNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopLearnerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_learner, parent, false)
        return TopLearnerViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopLearnerViewHolder, position: Int) {
        val learner = learners[position]

        // Устанавливаем номер места (начинается с 1)
        holder.number.text = (position + 1).toString()

        // Устанавливаем имя
        holder.name.text = learner.name

        // Устанавливаем количество уроков
        val lessonsText = when (learner.lessonsCompleted) {
            1 -> "1 урок"
            in 2..4 -> "${learner.lessonsCompleted} урока"
            else -> "${learner.lessonsCompleted} уроков"
        }
        holder.lessons.text = lessonsText

        // Загружаем аватар
        if (learner.avatarUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(learner.avatarUrl)
                .placeholder(R.drawable.ic_yo)
                .into(holder.avatar)
        } else {
            holder.avatar.setImageResource(R.drawable.ic_yo)
        }
    }

    override fun getItemCount(): Int = learners.size

    fun setData(newLearners: List<TopLearner>) {
        learners = newLearners
        notifyDataSetChanged()
    }
}