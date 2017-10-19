(ns re-val.subs
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]))

(rf/reg-sub
 :form-valid?
 (fn [db [_ id]]
   (let [fields (get-in db [:data id :options :fields])
         data (get-in db [:data id :data])
         validation (get-in db [:data id :validation])]
     (valid-form? fields data validation))))

(rf/reg-sub
 :get-form-data
 (fn [db [_ id]]
   (get-in db [:data id :data])))

(rf/reg-sub
 :get-form-field
 (fn [db [_ id k]]
   (get-in db [:data id :data k])))

(rf/reg-sub
 :get-validation-store
 (fn [db [_ id]]
   (get-in db [:data id :validation])))

(rf/reg-sub
 :get-form-validation
 (fn [[_ form-id _] _]
   [(rf/subscribe [:get-form-data form-id])
    (rf/subscribe [:get-validation-store form-id])
    (rf/subscribe [:get-form-options form-id])])
 (fn [[data validation-store {:keys [fields] :as options}] [_ form-id full?]]
   (if (= :full full?)
     ;;validate all fields
     (validate-all-fields (empty-validation-store fields) fields data)
     ;;else, validate against existing validation store
     validation-store)))

(rf/reg-sub
 :get-form-invalid-fields
 (fn [[_ form-id full?] _]
   (rf/subscribe [:get-form-validation form-id (when full? :full)]))
 (fn [validation]
   (:invalid-fields validation)))

(rf/reg-sub
 :get-form-options
 (fn [db [_ id]]
   (get-in db [:data id :options])))

(rf/reg-sub
 :get-form-fields
 (fn [db [_ id]]
   (get-in db [:data id :options :fields])))
