import { storeComponentBehavior } from '../../../../behaviors/index'
import { KITCHEN_ROLE_ENUM } from '../../../../common/enums/index'
import { userPresetDataApi, presetCategoryListApi, presetFoodListApi, presetSeasoningListApi } from '../../../../apis/index'

Component({
  behaviors: [storeComponentBehavior({ fields: ['kitchen'] })],
  properties: {
    stats: {
      type: Object,
      value: null
    }
  },
  data: {
    visible: false,
    // 分类（接口获取）
    categories: [],
    // 食材（按分类分组，接口获取）
    ingredientGroups: [],
    // 调料（按分类分组，接口获取）
    seasoningGroups: [],
  },
  observers: {
    stats(val) {
      if (!val) return
      const kitchen = this.data.kitchen?.active
      if (!kitchen || kitchen.role !== KITCHEN_ROLE_ENUM.OWNER) return
      const key = `guideSeen_${kitchen.id}`
      if (wx.getStorageSync(key)) return
      const { categoryCount, foodCount, seasoningCount } = val
      if (categoryCount === 0 && foodCount === 0 && seasoningCount === 0) {
        // 此时 stats 已加载（登录完成），首次展示时拉取预设选项
        if (!this.data.categories.length) this.getData()
        this.setData({ visible: true })
      }
    },
  },
  methods: {
    // 获取预设的分类/食材/调料选项
    async getData() {
      try {
        const [categoryRes, foodRes, seasoningRes] = await Promise.all([
          presetCategoryListApi(),
          presetFoodListApi(),
          presetSeasoningListApi(),
        ])
        this.setData({
          categories: (categoryRes.data || []).map(item => ({ ...item, selected: !!item.isDefault })),
          ingredientGroups: this._mapGroups(foodRes.data),
          seasoningGroups: this._mapGroups(seasoningRes.data),
        })
      } catch (e) {
        wx.$print('获取预设选项失败:', e)
      }
    },
    // 后端 GroupVO.list 映射为模板使用的 items，并按 isDefault 补充 selected
    _mapGroups(list) {
      return (list || []).map(group => ({
        name: group.name,
        items: (group.list || []).map(item => ({ ...item, selected: !!item.isDefault })),
      }))
    },
    _markSeen() {
      const kitchen = this.data.kitchen?.active
      if (!kitchen) return
      wx.setStorageSync(`guideSeen_${kitchen.id}`, true)
    },
    onClose() {
      this.setData({ visible: false })
    },
    onToggle(e) {
      const { type, id } = e.currentTarget.dataset
      if (type === 'categories') {
        const list = this.data.categories
        const index = list.findIndex(item => String(item.id) === String(id))
        if (index > -1) {
          this.setData({ [`categories[${index}].selected`]: !list[index].selected })
        }
      } else {
        // 食材和调料在分组中
        const groupsKey = type === 'ingredients' ? 'ingredientGroups' : 'seasoningGroups'
        const groups = this.data[groupsKey]
        for (let g = 0; g < groups.length; g++) {
          const items = groups[g].items
          const index = items.findIndex(item => String(item.id) === String(id))
          if (index > -1) {
            this.setData({ [`${groupsKey}[${g}].items[${index}].selected`]: !items[index].selected })
            break
          }
        }
      }
    },
    onSkip() {
      this._markSeen()
      this.setData({ visible: false })
    },
    async onConfirm() {
      const { categories, ingredientGroups, seasoningGroups } = this.data
      const selectedCategories = categories.filter(item => item.selected).map(i => i.name)
      // 预设分类必填，食材/调料选填
      if (!selectedCategories.length) {
        wx.$toast('请至少选择一个分类')
        return
      }
      this._markSeen()
      const foods = []
      ingredientGroups.forEach(g => g.items.forEach(i => i.selected && foods.push(i.name)))
      const seasonings = []
      seasoningGroups.forEach(g => g.items.forEach(i => i.selected && seasonings.push(i.name)))
      try {
        await userPresetDataApi({ categories: selectedCategories, foods, seasonings })
        this.triggerEvent('confirm')
      } catch (e) {
        wx.$print('预设数据失败:', e)
      }
      this.setData({ visible: false })
    }
  }
})
