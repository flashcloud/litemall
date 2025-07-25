<template>
  <div class="membership-plans">
    <div class="plan-row" v-for="(row, rowIndex) in planRows" :key="rowIndex">
      <plan-card
        v-for="plan in row"
        :key="plan.key"
        :plan-id="plan.id"
        :goods-id="plan.goodsId"
        :plan-key="plan.key"
        :title="plan.title"
        :current-price="plan.currentPrice"
        :original-price="plan.originalPrice"
        :tag="plan.tag"
        :note="plan.note"
        :plan-type="plan.planType"
        :is-selected="selectedPlan === plan.key"
        @select="handlePlanSelect"
      />
    </div>
  </div>
</template>

<script>
import PlanCard from '@/components/CustomTabs/PlanCard/index.vue';

export default {
  name: 'MembershipPlans',
  
  components: {
    PlanCard
  },
  
  props: {
    modelValue: {
      type: String,
      default: 'monthly'
    },
    // 套餐配置数据
    plans: {
      type: Array,
      default: () => []
    },
    // 每行显示的套餐数量
    plansPerRow: {
      type: Number,
      default: 2
    }
  },
  
  emits: ['update:modelValue', 'plan-change'],
  
  computed: {
    selectedPlan: {
      get() {
        return this.modelValue;
      },
      set(value) {
        this.$emit('update:modelValue', value);
      }
    },
    
    // 将套餐数据按行分组
    planRows() {
      const rows = [];
      for (let i = 0; i < this.plans.length; i += this.plansPerRow) {
        rows.push(this.plans.slice(i, i + this.plansPerRow));
      }
      return rows;
    }
  },
  
    methods: {
        handlePlanSelect(planData) {
            const planKey = planData.planKey;
            this.selectedPlan = planKey;

            //设置PlanCard的选中状态
            const planCards = this.$el.querySelectorAll('.plan-card');
            planCards.forEach(card => {
                card.classList.remove('active');
                if (card.getAttribute('data-plan-key') === planKey) {
                    card.classList.add('active');
                }
            });

            this.$emit('plan-change', planData);
        }
    }
};
</script>

<style scoped lang="scss">
.membership-plans {
  padding: 0 5px;
  margin-bottom: 16px;
  
  .plan-row {
    display: flex;
    gap: 12px;
    margin-bottom: 12px;
  }
}
</style>