import { store } from "../../store/index";
import { kitchenUserInviteApi } from "../../apis/index";
import { parseJsonToUrlParam, print } from "../../utils/index";
import { KITCHEN_ROLE_ENUM } from "../../common/enums/index";
import { OFFICIAL_TAGS, OFFICIAL_RECOMMEND_PATH, OFFICIAL_RECOMMEND_TITLE } from "../../common/constants/index";

// 分享文案池
const SHARE_TITLES = [
  "厨菜记 - 记录你的每一道菜",
  "今天做了什么好吃的？快来记录 ✨",
  "厨房管理，从厨菜记开始 🌟",
  "记录美食，分享美味 🙏",
  "来厨菜记，分享你的拿手菜",
  "美食记录，用心生活",
  "今天吃什么？一搜便知",
  "厨菜记，你的厨房好帮手",
];

// 随机获取分享文案
const getRandomTitle = () =>
  SHARE_TITLES[Math.floor(Math.random() * SHARE_TITLES.length)];

// 邀请文案
const INVITE_TITLES = {
  [KITCHEN_ROLE_ENUM.MANAGER]: (name) => `好友邀请你加入「${name}」当帮厨`,
  [KITCHEN_ROLE_ENUM.GUEST]: (name) => `好友邀请你加入「${name}」品尝美食`,
};

// 抽奖页分状态文案池（按本轮摇奖结果分流，每项可为字符串或接收动态参数的函数）
const DRAW_SHARE_TITLES = {
  idle: ["今天吃什么，交给命 🎲 点开也摇一把", "别纠结了，一梭子决定今天吃啥"],
  one: [
    (name) => `老虎机发话了：今天「${name}」`,
    (name) => `纠结半天，今天就吃「${name}」了 🎲`,
  ],
  multi: [
    (count) => `今日定餐小票 ${count} 道，全凭手气摇的 🧾`,
    (count) => `今天吃什么？我摇到了这 ${count} 道`,
  ],
};

// 随机取文案池一项（项为函数时传入动态参数）
const pickDrawTitle = (pool, arg) => {
  const item = pool[Math.floor(Math.random() * pool.length)];
  return typeof item === "function" ? item(arg) : item;
};

// 海报固定分状态文案（与随机分享文案分离，供抽菜海报页使用）
// 顶部只报数量、不出现菜名：菜名已由海报下方的拍立得卡片逐条展示，重复会挤掉标题重心
export const getPosterCopy = (pickedList = []) => {
  const count = Math.max(pickedList.length, 1);
  if (count <= 1) {
    return {
      title: `纠结半天，今天就吃这 ${count} 道了 🎲`,
      subTitle: "今天吃什么？一梭子摇出来的答案",
    };
  }
  return {
    title: `今天吃什么？我抽到了这 ${count} 道 🎲`,
    subTitle: "荤素汤齐，全凭手气摇的",
  };
};

// AI 荐菜页分享文案池（突出「按现有食材让 AI 配菜」的钩子）
const AI_SHARE_TITLES = [
  "今天吃什么？让 AI 帮你翻冰箱配菜 🧊",
  "冰箱就这点食材，AI 居然排出今晚吃啥 🤖",
  "AI 荐菜 · 看你现有食材能凑出哪几道",
  "别再为今晚吃啥发愁，AI 按食材给你配好了",
];

// 获取邀请人ID
const getInviterId = () => store.user?.id || "";

// 分享配置
export const sharePageBehavior = Behavior({
  methods: {
    _baseShare(params = {}) {
      params.inviterId = getInviterId();
      const query = parseJsonToUrlParam(params);
      const path = `/pages/index/index?${query}`;
      const title = params.title || getRandomTitle();
      const imageUrl = params.imageUrl || "/common/images/share.jpg";
      print("onShareAppMessage", { title, path, imageUrl });
      return { title, path, imageUrl };
    },

    async _inviteShare(id, name, role) {
      const r = await kitchenUserInviteApi({ kitchenId: id, role });
      const params = {};
      if (r.data) {
        params.title = INVITE_TITLES[role](name);
        params.kitchenId = id;
        params.code = r.data;
      }
      return this._baseShare(params);
    },

    // 菜谱专属分享配置
    _recipeShare(dish) {
      if (!dish || !dish.id) return this._baseShare();
      const name = dish.name || "";
      const title = `我做了一道${name}，来看看我的做法`;
      const path = `/subpackages/dish/pages/share/index?id=${dish.id}&inviterId=${getInviterId()}`;
      const imageUrl = dish.cover || "/common/images/dish-share.jpg";
      print("onShareAppMessage(recipe)", { title, path, imageUrl });
      return { title, path, imageUrl };
    },

    // 抽奖页按状态分享配置：摇奖结果由调用方传入 pickedList，落地页即抽奖页本身
    _drawShare(pickedList = []) {
      let title;
      if (pickedList.length === 1) {
        title = pickDrawTitle(DRAW_SHARE_TITLES.one, pickedList[0].name);
      } else if (pickedList.length > 1) {
        title = pickDrawTitle(DRAW_SHARE_TITLES.multi, pickedList.length);
      } else {
        title = pickDrawTitle(DRAW_SHARE_TITLES.idle);
      }
      const path = `/subpackages/dish/pages/draw/index?inviterId=${getInviterId()}`;
      const imageUrl = "/common/images/draw-share.jpg";
      print("onShare(draw)", { title, path, imageUrl });
      return { title, path, imageUrl };
    },

    // 抽奖页晒到朋友圈：标题复用分状态文案，query 携邀请人归因
    _drawTimeline(pickedList = []) {
      const { title } = this._drawShare(pickedList);
      return { title, query: `inviterId=${getInviterId()}` };
    },
    
    // AI 荐菜页分享配置：落地荐菜页本身，query 携邀请人归因
    _aiRecommendShare() {
      const title = pickDrawTitle(AI_SHARE_TITLES);
      const path = `/subpackages/dish/pages/ai-recommend/index?inviterId=${getInviterId()}`;
      const imageUrl = "/common/images/ai-recommend-share.jpg";
      print("onShare(ai-recommend)", { title, path, imageUrl });
      return { title, path, imageUrl };
    },

    // AI 荐菜页晒到朋友圈：标题同好友分享
    _aiRecommendTimeline() {
      const { title } = this._aiRecommendShare();
      return { title, query: `inviterId=${getInviterId()}` };
    },

    // 晒到公众号贴图（基础库 3.9.2+，必须由用户点击手势触发）
    async _postToOfficial({
      name,
      foods = [],
      seasonings = [],
      steps = [],
      remark = "",
      cover = "",
    }) {
      if (!wx.canIUse("shareToOfficialAccount")) {
        wx.$toast("当前微信版本不支持，升级后可用");
        return;
      }
      // 预填贴图正文：食材/调料/步骤/备注 + 固定导流尾巴
      const lines = [];
      if (foods.length)
        lines.push(
          `食材：${foods.map((f) => [f.name, f.amount].filter(Boolean).join(" ")).join("、")}`,
        );
      if (seasonings.length)
        lines.push(
          `调料：${seasonings.map((s) => [s.name, s.amount].filter(Boolean).join(" ")).join("、")}`,
        );
      if (steps.length)
        lines.push("做法：", ...steps.map((s, i) => `${i + 1}. ${s}`));
      if (remark) lines.push(`心得：${remark}`);
      lines.push("", "厨菜记 - 记录你的每一道菜");
      const images = cover ? await this._getShareImage(cover) : [];
      wx.shareToOfficialAccount({
        title: `今天做了一道「${name}」，来看看我的做法`,
        content: lines.join("\n"),
        tags: OFFICIAL_TAGS,
        images,
        recommendPath: OFFICIAL_RECOMMEND_PATH,
        recommendTitle: OFFICIAL_RECOMMEND_TITLE,
        success: (res) => {
          print("贴图发表成功", res);
          wx.$toast("感谢分享");
        },
        fail: (err) => {
          // 用户主动退出发表页也会走 fail，不提示
          print("贴图发表取消或失败", err);
        },
      });
    },
    // 网络图片转本地临时路径（贴图 images 仅接受本地/临时路径）
    _getShareImage(src) {
      return new Promise((resolve) => {
        wx.getImageInfo({
          src,
          success: ({ path }) => resolve([path]),
          fail: () => resolve([]),
        });
      });
    },

    // 分享给好友
    onShareAppMessage(e) {
      const { id, name, role, cover, type } = e.target?.dataset || {};
      print("onShareAppMessage", { id, name, role, type });
      // 菜谱分享
      if (type === "recipe") {
        return this._recipeShare({ id, name, cover });
      }
      // 邀请加入厨房
      if (type === "invite") {
        return this._inviteShare(id, name, role).catch(() => this._baseShare());
      }
      // 小程序通用分享（无 type）
      return this._baseShare();
    },
    // 分享到朋友圈
    onShareTimeline() {
      return {
        title: getRandomTitle(),
        query: `inviterId=${getInviterId()}`,
      };
    },
  },
});
